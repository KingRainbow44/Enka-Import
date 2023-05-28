package moe.seikimo.enka;

import emu.grasscutter.data.GameData;
import emu.grasscutter.game.avatar.Avatar;
import emu.grasscutter.game.inventory.GameItem;
import emu.grasscutter.game.player.Player;
import emu.grasscutter.game.props.ActionReason;
import emu.grasscutter.game.props.FightProperty;
import emu.grasscutter.server.packet.send.PacketAddNoGachaAvatarCardNotify;
import emu.grasscutter.server.packet.send.PacketAvatarAddNotify;
import emu.grasscutter.server.packet.send.PacketAvatarUnlockTalentNotify;
import emu.grasscutter.utils.JsonUtils;
import okhttp3.Request;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Collectors;

public interface Importer {
    Set<Integer> BAD_REQUEST = Set.of(400, 404, 424, 429, 500, 503);

    /**
     * Attempts to query the Enka Network API for a user's info.
     * Can return null if an error code is hit.
     *
     * @param userId The user's official UID.
     * @return The user's info, or null if the user was not found.
     */
    @Nullable
    static UserInfo queryUser(String userId) {
        var request = new Request.Builder()
                .url("https://enka.network/api/uid/" + userId)
                .addHeader("User-Agent", "KingRainbow44/Enka-Import")
                .build();

        try (var response = EnkaImport.getHttpClient()
                .newCall(request).execute()) {
            // Check if the response was successful.
            if (!response.isSuccessful() || BAD_REQUEST.contains(response.code())) {
                EnkaImport.getInstance().getLogger().debug("Invalid response code ({}) received.",
                        response.code());
                return null;
            }

            // Get the body of the response.
            var body = response.body();
            if (body == null) return null;
            // Get the body as a string.
            var content = body.string();
            // Close the body.
            body.close();

            // Parse the JSON.
            return JsonUtils.decode(content, UserInfo.class);
        } catch (IOException ignored) {
            return null;
        }
    }

    /**
     * Attempts to copy the data of a user to a player.
     *
     * @param player The player to copy the data to.
     * @param info The user's info.
     */
    static void importToPlayer(Player player, UserInfo info) {
        var logger = EnkaImport.getInstance().getLogger();
        logger.debug("Beginning import for {} (server UID: {})...",
                info.getUid(), player.getUid());

        // Fetch the basic avatar data.
        var basicData = info.getPlayerInfo().getShowAvatarInfoList();

        // Copy the player's avatars.
        var avatars = player.getAvatars();
        for (var avatarInfo : info.getAvatarInfoList()) {
            var avatarId = avatarInfo.getAvatarId();

            // Create the avatar, or fetch it if it doesn't exist.
            var newAvatar = !avatars.hasAvatar(avatarId);
            var avatar = newAvatar ? new Avatar(avatarId) :
                    avatars.getAvatarById(avatarId);
            if (newAvatar) {
                // Add the avatar to the player.
                avatars.addAvatar(avatar);
                player.sendPacket(new PacketAvatarAddNotify(
                        avatar, false));
                player.sendPacket(new PacketAddNoGachaAvatarCardNotify(
                        avatar, ActionReason.Gm));

                logger.debug("Added avatar {} to player {}.",
                        avatarId, player.getUid());
            }

            // Get the level of the avatar.
            var level = basicData.stream()
                    .filter(data -> data.getAvatarId() == avatarId)
                    .findFirst().orElse(new UserInfo.BasicAvatarInfo())
                    .getLevel();

            // Apply properties to the avatar.
            avatar.setLevel(level);
            avatar.setPromoteLevel(
                    Avatar.getMinPromoteLevel(level));
            avatar.setSkillDepotData(
                    GameData.getAvatarSkillDepotDataMap()
                            .get(avatar.getSkillDepotId()));
            logger.debug("Basic properties set. Level: {}; Ascension: {}.",
                    level, avatar.getPromoteLevel());

            // Remove existing constellations.
            avatar.forceConstellationLevel(-1);
            avatar.recalcConstellations();
            // Add new constellations.
            for (var talentId : avatarInfo.getTalentIdList()) {
                avatar.unlockConstellation(talentId, true);
                logger.debug("Unlocked constellation {} for avatar {}.",
                        talentId, avatarId);
            }

            for (var fightProp : avatarInfo.getFightPropMap().entrySet()) {
                avatar.setFightProperty(
                        FightProperty.getPropById(
                                Integer.parseInt(fightProp.getKey())),
                        fightProp.getValue()
                );
                logger.debug("Fight property {} was set to {}.",
                        fightProp.getKey(), fightProp.getValue());
            }
            for (var skill : avatarInfo.getSkillLevelMap().entrySet()) {
                avatar.setSkillLevel(
                        Integer.parseInt(skill.getKey()),
                        skill.getValue()
                );
                logger.debug("Skill {} was set to level {}.",
                        skill.getKey(), skill.getValue());
            }

            var equippedItems = new ArrayList<GameItem>();
            for (var equip : avatarInfo.getEquipList()) {
                var item = new GameItem(equip.getItemId());
                switch (item.getItemType()) {
                    default -> logger.debug("Invalid equipped item type.");
                    case ITEM_WEAPON -> {
                        var weaponInfo = equip.getWeapon();
                        item.setLevel(weaponInfo.getLevel());
                        item.setPromoteLevel(weaponInfo.getPromoteLevel());

                        // Determine the weapon's refinement level.
                        var affix = weaponInfo.getAffixMap();
                        var first = affix.entrySet().iterator().next();
                        var refinement = first.getValue();
                        item.setRefinement(refinement);

                        logger.debug("Weapon ({}) of level {} (ascension {}) has refinement {}.",
                                equip.getItemId(), weaponInfo.getLevel(), weaponInfo.getPromoteLevel(), refinement);
                    }
                    case ITEM_RELIQUARY -> {
                        var reliquaryInfo = equip.getReliquary();
                        item.setLevel(reliquaryInfo.getLevel());
                        item.setMainPropId(reliquaryInfo.getMainPropId());

                        var otherProps = item.getAppendPropIdList();
                        otherProps.clear();
                        otherProps.addAll(reliquaryInfo.getAppendPropIdList());

                        logger.debug("Artifact ({}) of level {} has a main stat of {} and sub-stats of {}.",
                                equip.getItemId(), reliquaryInfo.getLevel(), reliquaryInfo.getMainPropId(),
                                String.join(", ", reliquaryInfo.getAppendPropIdList().stream()
                                        .map(String::valueOf).toList()));
                    }
                }

                // Add the item to the player's inventory.
                player.getInventory().addItem(item, ActionReason.Gm);
                equippedItems.add(item);

                logger.debug("Added item {} to player {}.",
                        equip.getItemId(), player.getUid());
            }

            // Equip the items on the avatar.
            for (var item : equippedItems) {
                avatar.equipItem(item, false);
                logger.debug("Equipped item {} to avatar {}.",
                        item.getItemId(), avatar.getGuid());
            }

            // Finish the avatar.
            avatar.recalcStats();
            logger.debug("Finished avatar {}.", avatar.getGuid());

            // Equip the costume.
            var costume = avatarInfo.getCostumeId();
            if (costume != null) {
                avatars.changeCostume(avatar.getGuid(), costume);
                logger.debug("Equipped costume {} to avatar {}.",
                        costume, avatar.getGuid());
            }

            avatar.save();
            logger.debug("Saved avatar {}.", avatar.getGuid());
        }
    }
}
