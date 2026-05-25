package com.ontology.platform.config;

import com.ontology.platform.ontology.entity.CapabilityIndicator;
import com.ontology.platform.ontology.entity.CapabilityRelationship;
import com.ontology.platform.ontology.repository.OntologyRepository;
import com.ontology.platform.ontology.service.OntologySyncService;
import com.ontology.platform.rule.entity.RuleDefinition;
import com.ontology.platform.rule.repository.RuleRepository;
import com.ontology.platform.rule.service.DynamicRuleLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(1)
public class DataInitializer implements CommandLineRunner {

    private final OntologyRepository.IndicatorMapper indicatorMapper;
    private final OntologyRepository.RelationshipMapper relationshipMapper;
    private final RuleRepository ruleRepository;
    private final OntologySyncService syncService;
    private final DynamicRuleLoader dynamicRuleLoader;

    @Override
    public void run(String... args) {
        if (indicatorMapper.selectCount(null) > 0) {
            log.info("Ontology data already exists, skipping initialization");
            return;
        }

        log.info("========== Initializing ontology test data ==========");

        List<CapabilityIndicator> indicators = createIndicators();
        for (CapabilityIndicator ind : indicators) {
            indicatorMapper.insert(ind);
        }
        log.info("Created {} capability indicators", indicators.size());

        List<CapabilityRelationship> relationships = createRelationships(indicators);
        for (CapabilityRelationship rel : relationships) {
            relationshipMapper.insert(rel);
        }
        log.info("Created {} relationships", relationships.size());

        List<RuleDefinition> rules = createRules();
        for (RuleDefinition rule : rules) {
            ruleRepository.insert(rule);
        }
        log.info("Created {} DRL rules", rules.size());

        Map<String, String> activeRules = new java.util.HashMap<>();
        for (RuleDefinition rule : rules) {
            if ("ACTIVE".equals(rule.getStatus())) {
                activeRules.put(rule.getRuleName(), rule.getDrlContent());
            }
        }
        if (!activeRules.isEmpty()) {
            try {
                dynamicRuleLoader.loadAllRules(activeRules);
                log.info("Batch loaded {} ACTIVE rules into Drools engine", activeRules.size());
            } catch (Exception e) {
                log.warn("Failed to load rules: {}", e.getMessage());
            }
        }

        try {
            syncService.syncToNeo4j();
        } catch (Exception e) {
            log.warn("Neo4j sync skipped (Neo4j may not be available): {}", e.getMessage());
        }

        log.info("========== Ontology initialization complete ==========");
    }

    private List<CapabilityIndicator> createIndicators() {
        return List.of(
            // ===== 水面作战域 - 舰艇 =====
            ind("SurfaceWarfare", "Ship", "Destroyer-MaxSpeed", "驱逐舰最大航速", "节", 25.0, 35.0),
            ind("SurfaceWarfare", "Ship", "Destroyer-Displacement", "驱逐舰排水量", "吨", 3000.0, 12000.0),
            ind("SurfaceWarfare", "Ship", "Frigate-Endurance", "护卫舰续航力", "海里", 4000.0, 8000.0),
            ind("SurfaceWarfare", "Ship", "Carrier-AircraftCapacity", "航母舰载机容量", "架", 24.0, 90.0),

            // ===== 水面作战域 - 雷达 =====
            ind("SurfaceWarfare", "Radar", "Radar-DetectionRange", "雷达探测距离", "千米", 100.0, 500.0),
            ind("SurfaceWarfare", "Radar", "Radar-TrackCapacity", "雷达跟踪目标数", "个", 50.0, 500.0),
            ind("SurfaceWarfare", "Radar", "Radar-Resolution", "雷达分辨率", "米", 1.0, 50.0),
            ind("SurfaceWarfare", "Radar", "Radar-AntiJamming", "雷达抗干扰能力", "等级", 1.0, 10.0),

            // ===== 水面作战域 - 武器系统 =====
            ind("SurfaceWarfare", "WeaponSystem", "Weapon-Range", "武器射程", "千米", 10.0, 500.0),
            ind("SurfaceWarfare", "WeaponSystem", "Weapon-Accuracy", "火控精度", "概率", 0.7, 1.0),
            ind("SurfaceWarfare", "WeaponSystem", "Weapon-ReactionTime", "武器反应时间", "秒", 1.0, 30.0),
            ind("SurfaceWarfare", "WeaponSystem", "Weapon-LethalRadius", "杀伤半径", "米", 10.0, 500.0),

            // ===== 水下作战域 - 潜艇 =====
            ind("UnderwaterWarfare", "Submarine", "Submarine-DiveDepth", "潜艇下潜深度", "米", 200.0, 600.0),
            ind("UnderwaterWarfare", "Submarine", "Submarine-SilentLevel", "潜艇静音等级", "分贝", 90.0, 160.0),
            ind("UnderwaterWarfare", "Submarine", "Submarine-UnderwaterSpeed", "潜艇水下航速", "节", 10.0, 35.0),
            ind("UnderwaterWarfare", "Submarine", "Submarine-SubmergedDuration", "潜艇潜航时间", "小时", 48.0, 720.0),

            // ===== 水下作战域 - 水下探测 =====
            ind("UnderwaterWarfare", "UnderwaterDetection", "Sonar-DetectionRange", "声纳探测距离", "千米", 5.0, 100.0),
            ind("UnderwaterWarfare", "UnderwaterDetection", "Sonar-Resolution", "声纳分辨率", "度", 1.0, 30.0),
            ind("UnderwaterWarfare", "UnderwaterDetection", "Torpedo-Range", "鱼雷射程", "千米", 10.0, 50.0),
            ind("UnderwaterWarfare", "UnderwaterDetection", "Torpedo-Speed", "鱼雷航速", "节", 30.0, 60.0),

            // ===== 陆上作战域 - 坦克 =====
            ind("LandWarfare", "Tank", "Tank-ArmorThickness", "坦克装甲厚度", "毫米", 200.0, 1200.0),
            ind("LandWarfare", "Tank", "Tank-MaxSpeed", "坦克最大速度", "千米/时", 40.0, 80.0),
            ind("LandWarfare", "Tank", "Tank-MainGunCaliber", "坦克主炮口径", "毫米", 100.0, 155.0),
            ind("LandWarfare", "Tank", "Tank-OffRoadCapability", "坦克越野能力", "等级", 1.0, 10.0),

            // ===== 陆上作战域 - 火炮系统 =====
            ind("LandWarfare", "Artillery", "Artillery-Range", "火炮射程", "千米", 10.0, 100.0),
            ind("LandWarfare", "Artillery", "Artillery-FireRate", "火炮射速", "发/分", 2.0, 15.0),
            ind("LandWarfare", "Artillery", "Artillery-Accuracy", "火炮精度", "CEP米", 5.0, 200.0),

            // ===== 低空作战域 - 直升机 =====
            ind("LowAltitudeWarfare", "Helicopter", "Helicopter-MaxCeiling", "直升机最大升限", "米", 3000.0, 6000.0),
            ind("LowAltitudeWarfare", "Helicopter", "Helicopter-CruiseSpeed", "直升机巡航速度", "千米/时", 200.0, 350.0),
            ind("LowAltitudeWarfare", "Helicopter", "Helicopter-LiftCapacity", "直升机载重能力", "千克", 1000.0, 15000.0),
            ind("LowAltitudeWarfare", "Helicopter", "Helicopter-Endurance", "直升机续航时间", "小时", 2.0, 8.0),

            // ===== 低空作战域 - 无人机 =====
            ind("LowAltitudeWarfare", "UAV", "UAV-Endurance", "无人机续航时间", "小时", 4.0, 48.0),
            ind("LowAltitudeWarfare", "UAV", "UAV-ControlRange", "无人机控制距离", "千米", 10.0, 300.0),
            ind("LowAltitudeWarfare", "UAV", "UAV-Payload", "无人机载荷能力", "千克", 5.0, 500.0),
            ind("LowAltitudeWarfare", "UAV", "UAV-MaxAltitude", "无人机最大高度", "米", 3000.0, 12000.0),

            // ===== 高空作战域 - 歼击机 =====
            ind("HighAltitudeWarfare", "Fighter", "Fighter-MaxSpeed", "歼击机最大速度", "马赫", 1.5, 2.5),
            ind("HighAltitudeWarfare", "Fighter", "Fighter-CombatRadius", "歼击机作战半径", "千米", 500.0, 1500.0),
            ind("HighAltitudeWarfare", "Fighter", "Fighter-Ceiling", "歼击机升限", "米", 15000.0, 22000.0),
            ind("HighAltitudeWarfare", "Fighter", "Fighter-Payload", "歼击机载弹量", "千克", 3000.0, 12000.0),

            // ===== 高空作战域 - 轰炸机 =====
            ind("HighAltitudeWarfare", "Bomber", "Bomber-Range", "轰炸机航程", "千米", 3000.0, 12000.0),
            ind("HighAltitudeWarfare", "Bomber", "Bomber-Payload", "轰炸机载弹量", "千克", 10000.0, 40000.0),
            ind("HighAltitudeWarfare", "Bomber", "Bomber-Ceiling", "轰炸机升限", "米", 10000.0, 18000.0),

            // ===== 高空作战域 - 预警机 =====
            ind("HighAltitudeWarfare", "AWACS", "AWACS-DetectionRange", "预警机探测距离", "千米", 300.0, 600.0),
            ind("HighAltitudeWarfare", "AWACS", "AWACS-SimultaneousTracks", "预警机同时跟踪目标", "个", 100.0, 1000.0),
            ind("HighAltitudeWarfare", "AWACS", "AWACS-Endurance", "预警机续航时间", "小时", 6.0, 16.0),
            ind("HighAltitudeWarfare", "AWACS", "AWACS-DataProcessing", "预警机数据处理", "目标/秒", 100.0, 500.0)
        );
    }

    private List<CapabilityRelationship> createRelationships(
            List<CapabilityIndicator> indicators) {
        return List.of(
            // 雷达探测距离 → 直接影响 → 武器反应时间
            rel(indicators, "Radar-DetectionRange", "Weapon-ReactionTime",
                "directlyAffects", "RULE_RadarToWeaponReaction", 0.8, 10, "forward"),
            // 雷达探测距离 → 直接影响 → 武器射程
            rel(indicators, "Radar-DetectionRange", "Weapon-Range",
                "directlyAffects", "RULE_RadarToWeaponRange", 0.6, 8, "forward"),
            // 雷达分辨率 → 直接影响 → 火控精度
            rel(indicators, "Radar-Resolution", "Weapon-Accuracy",
                "directlyAffects", "RULE_ResolutionToAccuracy", 0.9, 15, "forward"),
            // 雷达跟踪目标数 → 直接影响 → 预警机同时跟踪目标
            rel(indicators, "Radar-TrackCapacity", "AWACS-SimultaneousTracks",
                "directlyAffects", "", 0.5, 5, "forward"),

            // 鱼雷航速 → 直接影响 → 潜艇水下航速
            rel(indicators, "Torpedo-Speed", "Submarine-UnderwaterSpeed",
                "directlyAffects", "RULE_TorpedoToSubSpeed", 0.7, 8, "forward"),
            // 声纳探测距离 → 间接影响 → 潜艇下潜深度
            rel(indicators, "Sonar-DetectionRange", "Submarine-DiveDepth",
                "indirectlyAffects", "", 0.4, 3, "forward"),
            // 声纳探测距离 → 直接影响 → 鱼雷射程
            rel(indicators, "Sonar-DetectionRange", "Torpedo-Range",
                "directlyAffects", "RULE_SonarToTorpedo", 0.75, 10, "forward"),

            // 坦克装甲厚度 → 直接影响 → 坦克越野能力
            rel(indicators, "Tank-ArmorThickness", "Tank-OffRoadCapability",
                "constrainedBy", "", -0.5, 5, "reverse"),
            // 坦克最大速度 → 阈值约束 → 坦克越野能力
            rel(indicators, "Tank-MaxSpeed", "Tank-OffRoadCapability",
                "constrainedBy", "", 0.3, 2, "forward"),
            // 火炮射程 → 直接影响 → 火炮精度
            rel(indicators, "Artillery-Range", "Artillery-Accuracy",
                "directlyAffects", "RULE_ArtilleryRangeToAccuracy", 0.65, 7, "forward"),

            // 直升机载重能力 → 直接影响 → 直升机续航时间
            rel(indicators, "Helicopter-LiftCapacity", "Helicopter-Endurance",
                "constrainedBy", "", -0.6, 4, "reverse"),
            // 无人机控制距离 → 直接影响 → 无人机最大高度
            rel(indicators, "UAV-ControlRange", "UAV-MaxAltitude",
                "directlyAffects", "", 0.55, 6, "forward"),

            // 歼击机速度 → 直接影响 → 歼击机作战半径
            rel(indicators, "Fighter-MaxSpeed", "Fighter-CombatRadius",
                "directlyAffects", "RULE_FighterSpeedToRadius", 0.7, 10, "forward"),
            // 歼击机载弹量 → 直接影响 → 轰炸机载弹量 (跨类别影响)
            rel(indicators, "Fighter-Payload", "Bomber-Payload",
                "indirectlyAffects", "", 0.3, 3, "forward"),
            // 预警机探测距离 → 直接影响 → 预警机数据处理
            rel(indicators, "AWACS-DetectionRange", "AWACS-DataProcessing",
                "directlyAffects", "RULE_AWACSDetectionToProcessing", 0.85, 12, "forward"),

            // 跨域影响：水下 → 水面
            rel(indicators, "Submarine-SilentLevel", "Radar-AntiJamming",
                "indirectlyAffects", "", 0.2, 2, "forward"),
            // 跨域影响：低空 → 高空
            rel(indicators, "Helicopter-MaxCeiling", "Fighter-Ceiling",
                "indirectlyAffects", "", 0.3, 3, "forward"),
            // 跨域影响：陆上 → 低空
            rel(indicators, "Artillery-Range", "UAV-ControlRange",
                "indirectlyAffects", "", 0.4, 3, "forward")
        );
    }

    private List<RuleDefinition> createRules() {
        return List.of(
            rule("RULE_RadarToWeaponReaction", "v1", "ACTIVE",
                "雷达,武器反应,水面", "雷达探测距离与武器反应时间的联动约束，探测距离不足时将导致武器反应时间超标",
                "package com.ontology.platform.rules;\n" +
                "import com.ontology.platform.rule.service.DroolsEngineService.IndicatorFact;\n" +
                "import com.ontology.platform.rule.service.DroolsEngineService.ViolationResult;\n" +
                "global java.util.List violations;\n\n" +
                "rule \"Radar Detection Range vs Weapon Reaction Time\"\n" +
                "    when\n" +
                "        $r : IndicatorFact(indicatorName == \"Radar-DetectionRange\", value != null)\n" +
                "        $w : IndicatorFact(indicatorName == \"Weapon-ReactionTime\", value != null)\n" +
                "    then\n" +
                "        double radar = ((Number) $r.value()).doubleValue();\n" +
                "        double reaction = ((Number) $w.value()).doubleValue();\n" +
                "        double expectedReaction = 35.0 - (radar / 500.0) * 30.0;\n" +
                "        if (reaction > expectedReaction + 5) {\n" +
                "            violations.add(new ViolationResult(\n" +
                "                \"Radar Detection Range vs Weapon Reaction Time\",\n" +
                "                \"Radar-DetectionRange\",\n" +
                "                \"雷达探测距离[\" + radar + \"km]不足导致武器反应时间[\" + reaction + \"s]超标，期望<[\" + String.format(\"%.1f\", expectedReaction) + \"s]\",\n" +
                "                0, expectedReaction, reaction));\n" +
                "        }\n" +
                "end\n"),

            rule("RULE_ResolutionToAccuracy", "v1", "ACTIVE",
                "雷达,火控精度,水面", "雷达分辨率直接影响火控系统命中精度，分辨率不足将导致精度低于期望值",
                "package com.ontology.platform.rules;\n" +
                "import com.ontology.platform.rule.service.DroolsEngineService.IndicatorFact;\n" +
                "import com.ontology.platform.rule.service.DroolsEngineService.ViolationResult;\n" +
                "global java.util.List violations;\n\n" +
                "rule \"Radar Resolution vs Weapon Accuracy\"\n" +
                "    when\n" +
                "        $res : IndicatorFact(indicatorName == \"Radar-Resolution\", value != null)\n" +
                "        $acc : IndicatorFact(indicatorName == \"Weapon-Accuracy\", value != null)\n" +
                "    then\n" +
                "        double resolution = ((Number) $res.value()).doubleValue();\n" +
                "        double accuracy = ((Number) $acc.value()).doubleValue();\n" +
                "        double expectedAccuracy = 1.0 - (resolution - 1.0) / 100.0;\n" +
                "        if (accuracy < expectedAccuracy - 0.05) {\n" +
                "            violations.add(new ViolationResult(\n" +
                "                \"Radar Resolution vs Weapon Accuracy\",\n" +
                "                \"Radar-Resolution\",\n" +
                "                \"雷达分辨率[\" + resolution + \"m]不足导致火控精度[\" + accuracy + \"]低于期望[\" + String.format(\"%.3f\", expectedAccuracy) + \"]\",\n" +
                "                expectedAccuracy - 0.05, 1.0, accuracy));\n" +
                "        }\n" +
                "end\n"),

            rule("RULE_TorpedoToSubSpeed", "v1", "ACTIVE",
                "鱼雷,潜艇,水下", "鱼雷航速与潜艇航速的追击约束，鱼雷速度必须大于潜艇速度的1.3倍",
                "package com.ontology.platform.rules;\n" +
                "import com.ontology.platform.rule.service.DroolsEngineService.IndicatorFact;\n" +
                "import com.ontology.platform.rule.service.DroolsEngineService.ViolationResult;\n" +
                "global java.util.List violations;\n\n" +
                "rule \"Torpedo Speed vs Submarine Speed\"\n" +
                "    when\n" +
                "        $t : IndicatorFact(indicatorName == \"Torpedo-Speed\", value != null)\n" +
                "        $s : IndicatorFact(indicatorName == \"Submarine-UnderwaterSpeed\", value != null)\n" +
                "    then\n" +
                "        double torpSpeed = ((Number) $t.value()).doubleValue();\n" +
                "        double subSpeed = ((Number) $s.value()).doubleValue();\n" +
                "        if (torpSpeed < subSpeed * 1.3) {\n" +
                "            violations.add(new ViolationResult(\n" +
                "                \"Torpedo Speed vs Submarine Speed\",\n" +
                "                \"Torpedo-Speed\",\n" +
                "                \"鱼雷航速[\" + torpSpeed + \"节]不足以有效追击潜艇[\" + subSpeed + \"节]，应大于[\" + String.format(\"%.1f\", subSpeed * 1.3) + \"节]\",\n" +
                "                subSpeed * 1.3, 60.0, torpSpeed));\n" +
                "        }\n" +
                "end\n"),

            rule("RULE_FighterSpeedToRadius", "v1", "ACTIVE",
                "歼击机,作战半径,高空", "歼击机最大速度与作战半径的匹配约束，速度越快期望作战半径越大",
                "package com.ontology.platform.rules;\n" +
                "import com.ontology.platform.rule.service.DroolsEngineService.IndicatorFact;\n" +
                "import com.ontology.platform.rule.service.DroolsEngineService.ViolationResult;\n" +
                "global java.util.List violations;\n\n" +
                "rule \"Fighter Speed vs Combat Radius\"\n" +
                "    when\n" +
                "        $speed : IndicatorFact(indicatorName == \"Fighter-MaxSpeed\", value != null)\n" +
                "        $radius : IndicatorFact(indicatorName == \"Fighter-CombatRadius\", value != null)\n" +
                "    then\n" +
                "        double speed = ((Number) $speed.value()).doubleValue();\n" +
                "        double radius = ((Number) $radius.value()).doubleValue();\n" +
                "        double expectedRadius = speed * 600.0;\n" +
                "        if (radius < expectedRadius * 0.8) {\n" +
                "            violations.add(new ViolationResult(\n" +
                "                \"Fighter Speed vs Combat Radius\",\n" +
                "                \"Fighter-MaxSpeed\",\n" +
                "                \"歼击机速度[\" + speed + \"马赫]与作战半径[\" + radius + \"km]不匹配，期望>[\" + String.format(\"%.0f\", expectedRadius * 0.8) + \"km]\",\n" +
                "                expectedRadius * 0.8, expectedRadius * 2.0, radius));\n" +
                "        }\n" +
                "end\n"),

            rule("RULE_SubmarineSilence", "v1", "ACTIVE",
                "潜艇,静音,隐身,水下", "潜艇静音等级阈值检查，超过140dB将失去隐身优势",
                "package com.ontology.platform.rules;\n" +
                "import com.ontology.platform.rule.service.DroolsEngineService.IndicatorFact;\n" +
                "import com.ontology.platform.rule.service.DroolsEngineService.ViolationResult;\n" +
                "global java.util.List violations;\n\n" +
                "rule \"Submarine Silent Level Check\"\n" +
                "    when\n" +
                "        $fact : IndicatorFact(indicatorName == \"Submarine-SilentLevel\", value != null)\n" +
                "    then\n" +
                "        double val = ((Number) $fact.value()).doubleValue();\n" +
                "        if (val > 140) {\n" +
                "            violations.add(new ViolationResult(\n" +
                "                \"Submarine Silent Level Check\",\n" +
                "                \"Submarine-SilentLevel\",\n" +
                "                \"潜艇静音等级[\" + val + \"dB]过高，超过隐身阈值140dB\",\n" +
                "                90, 140, val));\n" +
                "        }\n" +
                "end\n"),

            rule("RULE_TankArmorSpeed", "v1", "ACTIVE",
                "坦克,装甲,速度,陆上", "坦克装甲厚度与最大速度的权衡约束，装甲越厚期望速度越低",
                "package com.ontology.platform.rules;\n" +
                "import com.ontology.platform.rule.service.DroolsEngineService.IndicatorFact;\n" +
                "import com.ontology.platform.rule.service.DroolsEngineService.ViolationResult;\n" +
                "global java.util.List violations;\n\n" +
                "rule \"Tank Armor vs Speed Trade-off\"\n" +
                "    when\n" +
                "        $armor : IndicatorFact(indicatorName == \"Tank-ArmorThickness\", value != null)\n" +
                "        $speed : IndicatorFact(indicatorName == \"Tank-MaxSpeed\", value != null)\n" +
                "    then\n" +
                "        double armor = ((Number) $armor.value()).doubleValue();\n" +
                "        double speed = ((Number) $speed.value()).doubleValue();\n" +
                "        double maxExpectedSpeed = 85.0 - (armor - 200.0) * 0.04;\n" +
                "        if (speed > maxExpectedSpeed + 5) {\n" +
                "            violations.add(new ViolationResult(\n" +
                "                \"Tank Armor vs Speed Trade-off\",\n" +
                "                \"Tank-ArmorThickness\",\n" +
                "                \"装甲厚度[\" + armor + \"mm]下最大速度[\" + speed + \"km/h]超出物理极限，期望<[\" + String.format(\"%.1f\", maxExpectedSpeed) + \"km/h]\",\n" +
                "                40, maxExpectedSpeed, speed));\n" +
                "        }\n" +
                "end\n"),

            rule("RULE_AWACSEndurance", "v1", "ACTIVE",
                "预警机,续航,高空", "预警机续航时间最低要求检查，不得低于8小时",
                "package com.ontology.platform.rules;\n" +
                "import com.ontology.platform.rule.service.DroolsEngineService.IndicatorFact;\n" +
                "import com.ontology.platform.rule.service.DroolsEngineService.ViolationResult;\n" +
                "global java.util.List violations;\n\n" +
                "rule \"AWACS Endurance Check\"\n" +
                "    when\n" +
                "        $fact : IndicatorFact(indicatorName == \"AWACS-Endurance\", value != null)\n" +
                "    then\n" +
                "        double val = ((Number) $fact.value()).doubleValue();\n" +
                "        if (val < 8) {\n" +
                "            violations.add(new ViolationResult(\n" +
                "                \"AWACS Endurance Check\",\n" +
                "                \"AWACS-Endurance\",\n" +
                "                \"预警机续航时间[\" + val + \"h]不足，低于最低要求8小时\",\n" +
                "                8, 16, val));\n" +
                "        }\n" +
                "end\n")
        );
    }

    private CapabilityIndicator ind(String domain, String category,
                                     String name, String label, String unit,
                                     double min, double max) {
        CapabilityIndicator i = new CapabilityIndicator();
        i.setId(UUID.randomUUID().toString());
        i.setIri("http://www.ontology.org/capability#" + name);
        i.setName(label);
        i.setDomain(domain);
        i.setCategory(category);
        i.setUnit(unit);
        i.setThresholdMin(min);
        i.setThresholdMax(max);
        i.setDescription(label + " — 期望范围: " + min + " ~ " + max + " " + unit);
        return i;
    }

    private CapabilityRelationship rel(List<CapabilityIndicator> indicators,
                                        String srcName, String tgtName,
                                        String type, String ruleName,
                                        double weight, int priority, String direction) {
        CapabilityIndicator src = indicators.stream()
                .filter(i -> i.getIri().contains(srcName)).findFirst().orElseThrow();
        CapabilityIndicator tgt = indicators.stream()
                .filter(i -> i.getIri().contains(tgtName)).findFirst().orElseThrow();

        CapabilityRelationship r = new CapabilityRelationship();
        r.setId(UUID.randomUUID().toString());
        r.setSourceIndicatorId(src.getId());
        r.setTargetIndicatorId(tgt.getId());
        r.setRelationshipType(type);
        r.setDroolsRuleName(ruleName);
        r.setWeight(weight);
        r.setPriority(priority);
        r.setInfluenceDirection(direction);
        r.setEnabled(true);
        return r;
    }

    private RuleDefinition rule(String name, String version, String status,
                                  String tags, String description, String drl) {
        RuleDefinition r = new RuleDefinition();
        r.setId(UUID.randomUUID().toString());
        r.setRuleName(name);
        r.setVersion(version);
        r.setStatus(status);
        r.setTags(tags);
        r.setDescription(description);
        r.setDrlContent(drl);
        r.setPublishTime(status.equals("ACTIVE") ? LocalDateTime.now() : null);
        return r;
    }
}
