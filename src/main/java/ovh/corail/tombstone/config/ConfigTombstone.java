package ovh.corail.tombstone.config;

import com.google.common.collect.Lists;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import ovh.corail.tombstone.ModTombstone;
import ovh.corail.tombstone.block.BlockGraveMarble.MarbleType;
import ovh.corail.tombstone.block.GraveModel;
import ovh.corail.tombstone.helper.Helper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static ovh.corail.tombstone.ModTombstone.MOD_ID;

public class ConfigTombstone {

    public static class PlayerDeath {
        public final ForgeConfigSpec.ConfigValue<Boolean> handlePlayerDeath;
        public final ForgeConfigSpec.ConfigValue<Boolean> restoreEffectsOnDeath;
        public final ForgeConfigSpec.ConfigValue<Boolean> logPlayerGrave;
        public final ForgeConfigSpec.ConfigValue<Boolean> playerGraveAccess;
        public final ForgeConfigSpec.ConfigValue<Integer> snifferRange;
        public final ForgeConfigSpec.ConfigValue<List<String>> noGraveLocation;
        public final ForgeConfigSpec.ConfigValue<Integer> chanceMobOnGraveRecovery;
        public final ForgeConfigSpec.ConfigValue<Boolean> pvpMode;
        public final ForgeConfigSpec.ConfigValue<Integer> knowledgeLoss;
        public final ForgeConfigSpec.ConfigValue<Boolean> lossOnDeathOnlyForAbandonedGrave;
        public final ForgeConfigSpec.ConfigValue<Boolean> lossOnDeathOnlyForStackableItems;
        public final ForgeConfigSpec.ConfigValue<Integer> chanceLossOnDeath;
        public final ForgeConfigSpec.ConfigValue<Integer> percentLossOnDeath;
        public final ForgeConfigSpec.ConfigValue<Boolean> preventDeathOutsideWorld;
        public final ForgeConfigSpec.ConfigValue<Boolean> gravesBypassGriefingRules;
        public final ForgeConfigSpec.ConfigValue<Boolean> autoEquipOnDeathRespawn;
        public final ForgeConfigSpec.ConfigValue<Boolean> allowGraveInWater;
        public final ForgeConfigSpec.ConfigValue<Boolean> allowToFillExistingGrave;

        PlayerDeath(BuilderHandler builder) {
            builder.comment("Options related to player's death").push("player_death");
            handlePlayerDeath = builder.add("handle_player_death", "Whether to handle player death [false/true|default:true]", true);
            restoreEffectsOnDeath = builder.add("restore_effects_on_death", "Whether to restore beneficial effects after a player dies [false/true|default:true]", false);
            logPlayerGrave = builder.add("log_player_grave", "Whether to log the positions of players' graves [false/true|default:true]", true);
            playerGraveAccess = builder.add("player_grave_access", "Whether to require a Grave's Key to access graves [false/true|default:true]", true);
            snifferRange = builder.add("sniffer_range", "The radius in which items should be collected when a grave is spawned [1..10|default:5]", 5, 1, 10);
            noGraveLocation = builder.add("no_grave_location", "Graveless Areas", Lists.newArrayList("0, -300, 0, minecraft:overworld, 20"));
            chanceMobOnGraveRecovery = builder.add("chance_mob_on_grave_recovery", "The chance that creatures appear after the contents of a grave are retrieved [0..100|default:0]", 0, 0, 100);
            pvpMode = builder.add("pvp_mode", "Enables PvP mode, which allows players to open graves of players they kill [false/true|default:false]", false);
            knowledgeLoss = builder.add("knowledge_loss", "Knowledge of Death loss why dying [0..500|default:0]", 0, 0, 500);
            lossOnDeathOnlyForAbandonedGrave = builder.add("loss_on_death_only_for_abandoned_grave", "Only abandoned graves can have losses of items (based on the decay_time) [false/true|default:true]", true);
            lossOnDeathOnlyForStackableItems = builder.add("loss_on_death_only_for_stackable_items", "Only stackable items can be lost on death [false/true|default:true]", true);
            chanceLossOnDeath = builder.add("chance_loss_on_death", "The chance that some items are lost on death [0..100|default:0]", 0, 0, 100);
            percentLossOnDeath = builder.add("percent_loss_on_death", "The percentage of items that are lost on death [0..100|default:0]", 0, 0, 100);
            preventDeathOutsideWorld = builder.add("prevent_death_outside_world", "Prevents death outside of world borders [false/true|default:false]", false);
            gravesBypassGriefingRules = builder.add("graves_bypass_griefing_rules", "Whether graves should be allowed to bypass anti-griefing rules [false/true|default:true]", true);
            autoEquipOnDeathRespawn = builder.add("auto_equip_on_death_respawn", "Automatically equips any equipable items when respawning after a death [false/true|default:true]", true);
            allowGraveInWater = builder.add("allow_grave_in_water", "Allows player's graves to appear in water [false/true|default:true]", true);
            allowToFillExistingGrave = builder.add("allow_to_fill_existing_grave", "Allows to fill an existing grave instead of creating a new one [false/true|default:true]", true);
            builder.pop();
        }
    }

    public static class Alignment {
        public final ForgeConfigSpec.ConfigValue<Integer> pointsFreeSoulReceptacle;
        public final ForgeConfigSpec.ConfigValue<Integer> pointsPlunderPlayerGrave;
        public final ForgeConfigSpec.ConfigValue<Integer> pointsExorcismZombieVillager;
        public final ForgeConfigSpec.ConfigValue<Integer> pointsKillVillager;
        public final ForgeConfigSpec.ConfigValue<Integer> pointsKillRaider;
        public final ForgeConfigSpec.ConfigValue<Integer> chanceKillRaider;
        public final ForgeConfigSpec.ConfigValue<Integer> pointsTabletOfCupidity;
        public final ForgeConfigSpec.ConfigValue<Integer> chanceTabletOfCupidity;

        Alignment(BuilderHandler builder) {
            builder.comment("Options related to player's alignment").push("alignment");
            pointsFreeSoulReceptacle = builder.add("points_free_soul_receptacle", "Points for freeing a soul in a receptacle [0..50|default:3]", 3, 0, 50);
            pointsPlunderPlayerGrave = builder.add("points_plunder_player_grave", "Points for plundering a player's grave [-50..0|default:-2]", -2, -50, 0);
            pointsExorcismZombieVillager = builder.add("points_exorcism_zombie_villager", "Points for zombie villager exorcism [0..50|default:2]", 2, 0, 50);
            pointsKillVillager = builder.add("points_kill_villager", "Points for killing a villager (or minecolonies citizen) [0..50|default:-2]", -2, -50, 0);
            pointsKillRaider = builder.add("points_kill_raider", "Points for killing a raider [0..20|default:1]", 1, 0, 20);
            chanceKillRaider = builder.add("chance_kill_raider", "Chance for killing a raider [0..100|default:30]", 30, 0, 100);
            pointsTabletOfCupidity = builder.add("points_tablet_of_cupidity", "Points for tablet of cupidity [-20..0|default:-1]", -1, -20, 0);
            chanceTabletOfCupidity = builder.add("chance_tablet_of_cupidity", "Chance for tablet of cupidity [0..100|default:50]", 50, 0, 100);
            builder.pop();
        }
    }


    public static class General {
        public final ForgeConfigSpec.ConfigValue<Boolean> teleportDim;
        public final ForgeConfigSpec.ConfigValue<Boolean> nerfGhostlyShape;
        public final ForgeConfigSpec.ConfigValue<List<String>> unhandledBeneficialEffects;
        public final ForgeConfigSpec.ConfigValue<List<String>> unhandledHarmfulEffects;
        public final ForgeConfigSpec.ConfigValue<Boolean> knowledgeReducePhantomSpawn;
        public final ForgeConfigSpec.ConfigValue<Integer> timeForPhantomSpawn;
        public final ForgeConfigSpec.ConfigValue<Boolean> persistantHalloween;
        public final ForgeConfigSpec.ConfigValue<Boolean> allowBeyondTheGraveDamage;
        public final ForgeConfigSpec.ConfigValue<Integer> cooldownRequestTeleport;
        public final ForgeConfigSpec.ConfigValue<Integer> cooldownTeleportDeath;
        public final ForgeConfigSpec.ConfigValue<Integer> cooldownTeleportBind;
        public final ForgeConfigSpec.ConfigValue<Boolean> fishingTreasureInOpenWater;

        General(BuilderHandler builder) {
            builder.comment("Miscellaneous options").push("general");
            teleportDim = builder.add("teleport_dim", "Allows teleportation to other dimensions [false/true|default:true]", true);
            nerfGhostlyShape = builder.add("nerf_ghostly_shape", "Whether to cancel the Ghostly Shape effect on breaking a block or opening a chest [false/true|default:false]", false);
            unhandledBeneficialEffects = builder.add("unhandled_beneficial_effects", "Beneficial effects that can't used by certain features such as ankh of pray, lollipop, scroll of preservation, alchemy perk and magic siphon enchantment", Lists.newArrayList("tombstone:ghostly_shape", "tombstone:preservation", "tombstone:exorcism"));
            unhandledHarmfulEffects = builder.add("unhandled_harmful_effects", "Harmful effects that can't used by certain features such as tablet of cupidity and the enchantment plague bringer", Lists.newArrayList("minecraft:nausea"));
            knowledgeReducePhantomSpawn = builder.add("knowledge_reduce_phantom_spawn", "Increases the minimum time without sleeping for phantom spawn around player based on their level in Knowledge of Death [false/true|default:true]", true);
            timeForPhantomSpawn = builder.add("time_for_phantom_spawn", "Minimum time without sleeping for phantom to spawn around players [1200..MAX|default:72000]", 72000, 1200, Integer.MAX_VALUE);
            persistantHalloween = builder.add("persistant_halloween", "The Halloween features also happen outside the dates of the event [false/true|default:false]", false);
            allowBeyondTheGraveDamage = builder.add("allow_beyond_the_grave_damage", "Allows players to be hurt by special damages related to some items [false/true|default:true]", true);
            cooldownRequestTeleport = builder.add("cooldown_request_teleport", "Cooldown in minutes to use the command tbrequestteleport [-1..1440|default:-1]", -1, -1, 1440);
            cooldownTeleportDeath = builder.add("cooldown_teleport_death", "Cooldown in minutes to use the command tbteleportdeath [-1..1440|default:-1]", -1, -1, 1440);
            cooldownTeleportBind = builder.add("cooldown_teleport_bind", "Cooldown in minutes to use the command tbbind [-1..1440|default:-1]", -1, -1, 1440);
            fishingTreasureInOpenWater = builder.add("fishing_treasure_in_open_water", "Fishing treasures can only be found in open water [false/true|default:true]", true);
            builder.pop();
        }
    }

    public static class Enchantments {
        public final ForgeConfigSpec.ConfigValue<Boolean> restrictShadowStepToPlayer;
        public final ForgeConfigSpec.ConfigValue<Boolean> nerfPlagueBringer;
        public final ForgeConfigSpec.ConfigValue<Integer> maxDurationMagicSiphon;
        public final ForgeConfigSpec.ConfigValue<Integer> durationPlagueBringer;

        Enchantments(BuilderHandler builder) {
            builder.comment("Allows to customize or disable the enchantments").push("enchantments");
            restrictShadowStepToPlayer = builder.add("restrict_shadow_step_to_player", "Restricts the effect of Shadow Step to players [false/true|default:true]", true);
            nerfPlagueBringer = builder.add("nerf_plague_bringer", "The wearer is also affected by the effect if his alignment is not bad [false/true|default:true]", true);
            maxDurationMagicSiphon = builder.add("max_duration_magic_siphon", "Maximum duration of stolen effects in minutes [1..MAX|default:1440]", 1440, 1, Integer.MAX_VALUE);
            durationPlagueBringer = builder.add("duration_plague_bringer", "Duration of applied effects in seconds [1..MAX|default:10]", 10, 1, Integer.MAX_VALUE);
            builder.pop();
        }
    }

    public static class DecorativeGrave {
        public final ForgeConfigSpec.ConfigValue<Integer> timeSoul;
        public final ForgeConfigSpec.ConfigValue<Integer> chanceSoul;
        public final ForgeConfigSpec.ConfigValue<Integer> chancePrayReward;
        public final ForgeConfigSpec.ConfigValue<Boolean> lostTabletSearchOutsideWorld;
        public final ForgeConfigSpec.ConfigValue<Boolean> lostTabletSearchModdedStructure;
        public final ForgeConfigSpec.ConfigValue<List<String>> lostTabletDeniedStructures;
        public final ForgeConfigSpec.ConfigValue<List<String>> blackListCapturableSouls;
        public final ForgeConfigSpec.ConfigValue<Boolean> purificationAffectNeutralEffects;
        public final ForgeConfigSpec.ConfigValue<Integer> durationVoodooPoppetEffects;

        DecorativeGrave(BuilderHandler builder) {
            builder.comment("For settings related to decorative tombs and magic items").push("decorative_grave");
            timeSoul = builder.add("time_soul", "Time in minutes to check if a soul appears on a grave [10..10000|default:30]", 30, 10, 10000);
            chanceSoul = builder.add("chance_soul", "Chance on 1000 that a soul appears on a grave [0..1000|default:100]", 100, 0, 1000);
            chancePrayReward = builder.add("chance_pray_reward", "Chance to receive a random beneficial spell effect when praying near a grave [0..100|default:40]", 40, 0, 100);
            lostTabletSearchOutsideWorld = builder.add("lost_tablet_search_outside_world", "Allows lost tablets to find locations outside the current world [false/true|default:true]", true);
            lostTabletSearchModdedStructure = builder.add("lost_tablet_modded_structure", "Allows lost tablets to find modded structures [false/true|default:true]", true);
            lostTabletDeniedStructures = builder.add("lost_tablet_denied_structures", "The structures that can't be discovered by lost tablets", new ArrayList<>());
            blackListCapturableSouls = builder.add("black_list_capturable_souls", "The creatures that can't be captured in receptacle", new ArrayList<>());
            purificationAffectNeutralEffects = builder.add("purification_affect_neutral_effects", "Allows the purification effect to clear neutral effects [false/true|default:true]", true);
            durationVoodooPoppetEffects = builder.add("duration_voodoo_poppet_effects", "Duration of voodoo poppet's effects in seconds when preventing death [5..60000|default:60]", 60, 5, 60000);
            builder.pop();
        }
    }

    public static class Client {
        public enum FogDensity {NONE, LOW, NORMAL, HIGH}

        public enum FogPeriod implements Predicate<World> {
            DAY(Helper::isDay), NIGHT(Helper::isNight), BOTH(world -> true), NEVER(world -> false);
            private final Predicate<World> predic;

            FogPeriod(Predicate<World> predic) {
                this.predic = predic;
            }

            @Override
            public boolean test(World world) {
                return this.predic.test(world);
            }
        }

        public enum GraveSkinRule {DEFAULT, FORCE_NORMAL, FORCE_HALLOWEEN}

        public final ForgeConfigSpec.ConfigValue<FogDensity> fogDensity;
        public final ForgeConfigSpec.ConfigValue<Integer> particleCastingColor;
        public final ForgeConfigSpec.ConfigValue<Boolean> showMagicCircle;
        public final ForgeConfigSpec.ConfigValue<Boolean> showEnhancedTooltips;
        public final ForgeConfigSpec.ConfigValue<Boolean> highlight;
        public final ForgeConfigSpec.ConfigValue<Boolean> skipRespawnScreen;
        public final ForgeConfigSpec.ConfigValue<Boolean> showShadowStep;
        public final ForgeConfigSpec.ConfigValue<Boolean> enableHalloweenEffect;
        public final ForgeConfigSpec.ConfigValue<Integer> textColorDeathDate;
        public final ForgeConfigSpec.ConfigValue<Integer> textColorRIP;
        public final ForgeConfigSpec.ConfigValue<Integer> textColorOwner;
        public final ForgeConfigSpec.ConfigValue<Boolean> dateInMCTime;
        public final ForgeConfigSpec.ConfigValue<Boolean> displayKnowledgeMessage;
        public final ForgeConfigSpec.ConfigValue<Boolean> equipElytraInPriority;
        public final ForgeConfigSpec.ConfigValue<GraveModel> favoriteGrave;
        public final ForgeConfigSpec.ConfigValue<MarbleType> favoriteGraveMarble;
        public final ForgeConfigSpec.ConfigValue<Boolean> showInfoOnEnchantment;
        public final ForgeConfigSpec.ConfigValue<GraveSkinRule> graveSkinRule;
        public final ForgeConfigSpec.ConfigValue<Boolean> priorizeToolOnHotbar;
        public final ForgeConfigSpec.ConfigValue<Integer> fogColor;
        public final ForgeConfigSpec.ConfigValue<FogPeriod> fogPeriod;
        public final ForgeConfigSpec.ConfigValue<Boolean> activateGraveBySneaking;

        Client(BuilderHandler builder) {
            builder.comment("Personal Options that can be edited even on server").push("client");
            fogDensity = builder.addEnum("fog_density", "Fog density around the graves [NONE/LOW/NORMAL/HIGH|default:LOW]", FogDensity.LOW);
            particleCastingColor = builder.add("particle_casting_color", "Decimal value for the color of the particles when using magic items [0..16777215|default:125656]", 14937088, 0, 16777215);
            showMagicCircle = builder.add("show_magic_circle", "Shows the magic circles when using some items [false/true|default:true]", true);
            showEnhancedTooltips = builder.add("show_enhanced_tooltips", "Shows all the infos in the tooltip of items [false/true|default:true]", true);
            highlight = builder.add("highlight", "Highlights the tomb from far when holding the key [false/true|default:true]", true);
            skipRespawnScreen = builder.add("skip_respawn_screen", "Skips the Respawn Screen [false/true|default:false]", false);
            showShadowStep = builder.add("hide_shadow_step", "Shows shadow step particles [false/true|default:true]", true);
            enableHalloweenEffect = builder.add("enable_halloween_effect", "Enables the special rendering and particles for Halloween [false/true|default:true]", true);
            textColorDeathDate = builder.add("text_color_death_date", "Decimal value for the color of the grave text <Death Date> [0..16777215|default:2962496]", 2962496, 0, 16777215);
            textColorRIP = builder.add("text_color_rip", "Decimal value for the color of the grave text <R.I.P.> [0..16777215|default:2962496]", 2962496, 0, 16777215);
            textColorOwner = builder.add("text_color_owner", "Decimal value for the color of the grave text <Owner Name> [0..16777215|default:5991302]", 5991302, 0, 16777215);
            dateInMCTime = builder.add("date_in_mc_time", "Shows only the elapsed minecraft days since the death on graves [false/true|default:false]", false);
            displayKnowledgeMessage = builder.add("display_knowledge_message", "Display or not the messages of gain of points in knowledge of death [false/true|default:true]", true);
            equipElytraInPriority = builder.add("equip_elytra_in_priority", "Equips elytra in priority when recovering your lost items [false/true|default:false]", false);
            favoriteGrave = builder.addEnum("favorite_grave", "Favorite grave", GraveModel.GRAVE_SIMPLE);
            favoriteGraveMarble = builder.addEnum("favorite_grave_marble", "Favorite grave marble", MarbleType.DARK);
            showInfoOnEnchantment = builder.add("show_info_on_enchantment", "Shows the use of the Tombstone's enchantments in tooltip [false/true|default:true]", true);
            graveSkinRule = builder.addEnum("grave_skin_rule", "Defines the rule to use for grave's skin [DEFAULT/FORCE_NORMAL/FORCE_HALLOWEEN|default:DEFAULT]", GraveSkinRule.DEFAULT);
            priorizeToolOnHotbar = builder.add("priorize_tool_on_hotbar", "Favor the tools on the hotbar when recovering a grave [false/true|default:false]", false);
            fogColor = builder.add("fog_color", "Decimal value of the fog color [0..16777215|default:125656]", 16777215, 0, 16777215);
            fogPeriod = builder.addEnum("fog_period", "Period where graves produce fog [DAY/NIGHT/BOTH|default:NIGHT]", FogPeriod.NIGHT);
            activateGraveBySneaking = builder.add("activate_grave_by_sneaking", "Allows to activate a grave by sneaking [false/true|default:true]", true);
            builder.pop();
        }
    }

    public static class Loot {
        public final ForgeConfigSpec.ConfigValue<Integer> chanceDecorativeGraveOnBoss;
        public final ForgeConfigSpec.ConfigValue<Integer> chanceRandomPoppetOnBoss;
        public final ForgeConfigSpec.ConfigValue<Integer> chanceRandomScrollOnBoss;
        public final ForgeConfigSpec.ConfigValue<Boolean> undeadCanDropSkull;
        public final ForgeConfigSpec.ConfigValue<Integer> chanceSoulReceptacleOnBoss;
        public final ForgeConfigSpec.ConfigValue<Integer> chanceGraveDust;
        public final ForgeConfigSpec.ConfigValue<List<String>> treasureLootTable;

        Loot(BuilderHandler builder) {
            builder.comment("Allows to change the chance to drop some items").push("loot");
            chanceDecorativeGraveOnBoss = builder.add("chance_decorative_grave_on_boss", "Chance on 1000 to receive a decorative grave on undead boss [0..1000|default:50]", 30, 0, 1000);
            chanceRandomPoppetOnBoss = builder.add("chance_random_poppet_on_boss", "Chance on 1000 to receive a random poppet on undead boss [0..1000|default:100]", 100, 0, 1000);
            chanceRandomScrollOnBoss = builder.add("chance_random_scroll_on_boss", "Chance on 1000 to receive a random scroll on undead boss [0..1000|default:100]", 100, 0, 1000);
            undeadCanDropSkull = builder.add("undead_can_drop_skull", "Allows the undeads to have a low chance to drop their skull [false/true|default:true]", true);
            chanceSoulReceptacleOnBoss = builder.add("chance_soul_receptacle_on_boss", "Chance on 1000 to receive a Soul Receptacle on undead boss [0..1000|default:50]", 50, 0, 1000);
            chanceGraveDust = builder.add("chance_grave_dust", "Chance on 1000 for undead mobs to drop Grave's Dust [0..1000|default:100]", 100, 0, 1000);
            treasureLootTable = builder.add("treasure_loot_table", "Defines the allowed loottables having a chance to contain a magic item from Tombstone", Lists.newArrayList("minecraft:chests/end_city_treasure", "minecraft:chests/abandoned_mineshaft", "minecraft:chests/nether_bridge", "minecraft:chests/stronghold_library", "minecraft:chests/desert_pyramid", "minecraft:chests/jungle_temple", "minecraft:chests/igloo_chest", "minecraft:chests/woodland_mansion"));
            builder.pop();
        }
    }

    public static class Recovery {
        public final ForgeConfigSpec.ConfigValue<Boolean> recoveryPlayerEnable;
        public final ForgeConfigSpec.ConfigValue<Integer> recoveryPlayerTimer;
        public final ForgeConfigSpec.ConfigValue<Integer> recoveryPlayerMaxSaves;
        public final ForgeConfigSpec.ConfigValue<Boolean> recoveryFamiliarEnable;
        public final ForgeConfigSpec.ConfigValue<Boolean> backupOnDeath;

        Recovery(BuilderHandler builder) {
            builder.comment("Options related to the command recovery and auto-save of players").push("recovery");
            recoveryPlayerEnable = builder.add("recovery_player_enable", "Enables to backup automatically players [false/true|default:true]", true);
            recoveryPlayerTimer = builder.add("recovery_player_timer", "Time in minutes between players' backups [10..1000|default:40]", 20, 5, 1000);
            recoveryPlayerMaxSaves = builder.add("recovery_player_max_saves", "Maximum number of backups per player [5..100|default:15]", 15, 5, 100);
            recoveryFamiliarEnable = builder.add("recovery_familiar_enable", "Enables to backup automatically dead familiars [false/true|default:true]", true);
            backupOnDeath = builder.add("backup_on_death", "Backup players on death [false/true|default:false]", false);
            builder.pop();
        }
    }

    public static class VillageSiege {
        public final ForgeConfigSpec.ConfigValue<Boolean> handleVillageSiege;
        public final ForgeConfigSpec.ConfigValue<Boolean> logSiegeState;
        public final ForgeConfigSpec.ConfigValue<Boolean> glowingCreatureTest;
        public final ForgeConfigSpec.ConfigValue<Boolean> allowCreativePlayersForSiege;
        public final ForgeConfigSpec.ConfigValue<Integer> siegeChance;
        public final ForgeConfigSpec.ConfigValue<Integer> siegeMaxCreature;
        public final ForgeConfigSpec.ConfigValue<Boolean> undeadWearHelmInSiege;
        public final ForgeConfigSpec.ConfigValue<Integer> delaySiegeTest;
        public final ForgeConfigSpec.ConfigValue<Boolean> persistentMobInSiege;
        public final ForgeConfigSpec.ConfigValue<Boolean> shufflePlayersForSiege;

        VillageSiege(BuilderHandler builder) {
            builder.comment("Allows to define the conditions for a village siege to begin").push("village_siege");
            handleVillageSiege = builder.add("handle_village_siege", "Allows to handle village sieges [false/true|default:true]", true);
            logSiegeState = builder.add("log_siege_state", "Logs the different states of a village siege while searching for an adequate place [false/true|default:false]", false);
            glowingCreatureTest = builder.add("glowing_creature_test", "The creatures of the siege have a glowing effect (only uses this for test purposes) [false/true|default:false]", false);
            allowCreativePlayersForSiege = builder.add("allow_creative_players_for_siege", "Allows to use the positions of creative players to define the siege location [false/true|default:true]", true);
            siegeChance = builder.add("siege_chance", "Chance for a siege to occur [0..100|default:10]", 10, 0, 100);
            siegeMaxCreature = builder.add("siege_max_creature", "Maximum of creatures appearing in a siege [0..100|default:20]", 20, 0, 100);
            undeadWearHelmInSiege = builder.add("undead_wear_helm_in_siege", "Undeads always wear a helm when sieging [false/true|default:false]", false);
            delaySiegeTest = builder.add("delay_siege_test", "Delay in seconds for a second test of siege when the first failed [0..2400|default:600]", 600, 0, 2400);
            persistentMobInSiege = builder.add("persistent_mob_in_siege", "Mobs in siege are persistent [false/true|default:false]", false);
            shufflePlayersForSiege = builder.add("shuffle_players_for_siege", "Shuffles the list of players before testing the siege location [false/true|default:true]", true);
            builder.pop();
        }
    }

    public static class Compatibility {
        public final ForgeConfigSpec.ConfigValue<Boolean> allowCurioAutoEquip;
        public final ForgeConfigSpec.ConfigValue<Boolean> keepCosmeticArmor;
        public final ForgeConfigSpec.ConfigValue<Boolean> disableDisenchantmentForTetra;

        Compatibility(BuilderHandler builder) {
            builder.comment("Allows to enable some features related to others mods").push("compatibility");
            allowCurioAutoEquip = builder.add("allow_curio_auto_equip", "Allows to auto-equip the slots from Curio mod [false/true|default:true]", true);
            keepCosmeticArmor = builder.add("keep_cosmetic_armor", "Keeps the cosmetic armor when you die [false/true|default:true]", true);
            disableDisenchantmentForTetra = builder.add("disable_disenchantment_for_tetra", "Disables to disenchant items from tetra mod [false/true|default:false]", false);
            builder.pop();
        }
    }

    private static String getTranslation(String name) {
        return MOD_ID + ".config." + name;
    }

    public static final Client client;

    public static final ForgeConfigSpec CLIENT_SPEC;

    static {
        BuilderHandler BUILDER = new BuilderHandler();
        client = new Client(BUILDER);
        CLIENT_SPEC = BUILDER.build();
    }

    public static final PlayerDeath player_death;
    public static final Alignment alignment;
    public static final General general;
    public static final VillageSiege village_siege;
    public static final DecorativeGrave decorative_grave;
    public static final Enchantments enchantments;
    public static final Recovery recovery;
    public static final Loot loot;
    public static final Compatibility compatibility;

    public static final ForgeConfigSpec GENERAL_SPEC;

    static {
        BuilderHandler BUILDER = new BuilderHandler();
        player_death = new PlayerDeath(BUILDER);
        alignment = new Alignment(BUILDER);
        general = new General(BUILDER);
        village_siege = new VillageSiege(BUILDER);
        decorative_grave = new DecorativeGrave(BUILDER);
        enchantments = new Enchantments(BUILDER);
        recovery = new Recovery(BUILDER);
        loot = new Loot(BUILDER);
        compatibility = new Compatibility(BUILDER);
        GENERAL_SPEC = BUILDER.build();
    }

    private static class BuilderHandler extends ForgeConfigSpec.Builder {
        private ForgeConfigSpec.ConfigValue<List<String>> add(String name, String comment, List<String> defaultVal) {
            return comment(comment).translation(getTranslation(name)).define(name, defaultVal);
        }

        private <T extends Enum<T>> ForgeConfigSpec.EnumValue<T> addEnum(String name, String comment, T defaultVal) {
            return comment(comment).translation(getTranslation(name)).defineEnum(name, defaultVal);
        }

        private ForgeConfigSpec.ConfigValue<Boolean> add(String name, String comment, boolean defaultVal) {
            return add(name, comment, getTranslation(name), defaultVal);
        }

        private ForgeConfigSpec.ConfigValue<Boolean> add(String name, String comment, String translation, boolean defaultVal) {
            return comment(comment).translation(translation).define(name, defaultVal);
        }

        private ForgeConfigSpec.ConfigValue<Integer> add(String name, String comment, int defaultVal, int minVal, int maxVal) {
            return comment(comment).translation(getTranslation(name)).defineInRange(name, defaultVal, minVal, maxVal);
        }
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ConfigEvent {
        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void onReloadConfig(ModConfig.Reloading event) {
            if (event.getConfig().getModId().equals(MOD_ID) && (event.getConfig().getType() == ModConfig.Type.CLIENT || event.getConfig().getType() == ModConfig.Type.SERVER)) {
                ModTombstone.PROXY.markConfigDirty();
            }
        }
    }
}
