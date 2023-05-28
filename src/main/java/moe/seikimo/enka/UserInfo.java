package moe.seikimo.enka;

import com.google.gson.annotations.SerializedName;
import emu.grasscutter.game.inventory.EquipType;
import emu.grasscutter.game.inventory.ItemType;
import emu.grasscutter.game.props.FightProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public final class UserInfo {
    private PlayerInfo playerInfo = new PlayerInfo();
    private List<AvatarInfo> avatarInfoList = List.of();

    @SerializedName("ttl")
    private int refreshIn = 0;
    private String uid;

    @Data
    public static class PlayerInfo {
        private String nickname = "";
        private int level = 0;
        private String signature = "";
        private int worldLevel = 0;
        private int nameCardId = 0;
        private int finishAchievementNum = 0;
        private int towerFloorIndex = 0;
        private int towerLevelIndex = 0;
        private List<BasicAvatarInfo> showAvatarInfoList = List.of();
        private List<Integer> showNameCardIdList = List.of();
        private ProfilePicture profilePicture = new ProfilePicture();
    }

    @Data
    public static class BasicAvatarInfo {
        private int avatarId = 0;
        private int level = 0;
    }

    @Data
    public static class ProfilePicture {
        public int avatarId = 0;
    }

    @Data
    public static class AvatarInfo {
        private int avatarId = 0; // Used.
        private Integer costumeId = 0; // Used.
        private Map<String, PropertyValue> propMap = Map.of();
        private List<Integer> talentIdList = List.of();
        private Map<String, Float> fightPropMap = Map.of(); // Used.
        private int skillDepotId = 0; // Used.
        private List<Integer> inherentProudSkillList = List.of();
        private Map<String, Integer> skillLevelMap = Map.of(); // Used.
        private Map<String, Integer> proudSkillExtraLevelMap = Map.of();
        private List<EquipInfo> equipList = List.of(); // Used.
        private FriendshipInfo fetterInfo = new FriendshipInfo(); // Used.
    }

    @Data
    public static class PropertyValue {
        public int type = 0;
        public String val = "";
        public String ival = this.val;
    }

    @Data
    public static class EquipInfo {
        private int itemId = 0;
        private ReliquaryInfo reliquary = new ReliquaryInfo();
        private WeaponInfo weapon = new WeaponInfo();

        @SerializedName("flat")
        private ItemInfo itemInfo = new ItemInfo();
    }

    @Data
    public static class ReliquaryInfo {
        private int level = 0;
        private int mainPropId = 0;
        private List<Integer> appendPropIdList = List.of();
    }

    @Data
    public static class WeaponInfo {
        private int level = 0;
        private int promoteLevel = 0;
        private Map<String, Integer> affixMap = Map.of();
    }

    @Data
    public static class ItemInfo {
        private String nameTextMapHash = "";
        private String setNameTextMapHash = "";
        private int rankLevel = 0;
        private ReliquaryProperty reliquaryMainstat = new ReliquaryProperty();
        private List<ReliquaryProperty> reliquarySubstats = List.of();
        private List<WeaponProperty> weaponStats = List.of();
        private ItemType itemType = ItemType.ITEM_NONE;
        private String icon = "";
        private EquipType equipType = EquipType.EQUIP_NONE;
    }

    @Data
    public static class ReliquaryProperty {
        private FightProperty mainPropId = FightProperty.FIGHT_PROP_NONE;
        private float statValue = 0;
    }

    @Data
    public static class WeaponProperty {
        private FightProperty appendPropId = FightProperty.FIGHT_PROP_NONE;
        private float statValue = 0;
    }

    @Data
    public static class FriendshipInfo {
        @SerializedName("expLevel")
        private int level = 0;
    }
}
