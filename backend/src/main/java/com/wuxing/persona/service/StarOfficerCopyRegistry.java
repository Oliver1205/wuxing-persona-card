package com.wuxing.persona.service;

final class StarOfficerCopyRegistry {

    private StarOfficerCopyRegistry() {
    }

    static StarLineage lineage(StarOfficer starOfficer) {
        return switch (starOfficer.getCode()) {
            case "JIAO_XIU" -> new StarLineage("东方青龙七宿第一宿", "春木初生、开端、伸展和方向感", "角有开始、探出和打开局面的意味");
            case "FANG_XIU" -> new StarLineage("东方青龙七宿第四宿", "生长进入结构、位置、规划和安顿", "房带空间、容纳和组织感");
            case "JI_XIU" -> new StarLineage("东方青龙七宿第七宿", "风、流动、引导和把方向吹开的力量", "箕有筛分、疏导和把杂乱理顺的意味");
            case "JING_XIU" -> new StarLineage("南方朱雀七宿第一宿", "火气升起、资源汇聚、行动和外放", "井有水源、秩序和把资源安置好的意味");
            case "XING_XIU" -> new StarLineage("南方朱雀七宿第四宿", "明亮、显现、表达和被看见", "星本身带光点、辨识度和抬头可见的意味");
            case "ZHANG_XIU" -> new StarLineage("南方朱雀七宿第五宿", "舒展、铺开、承接热度后的展开", "张有展开、拉开格局和把气势铺出去的意味");
            case "KUI_XIU" -> new StarLineage("西方白虎七宿第一宿", "秋金初起、文气、边界和标准", "奎常被放在文章、纹理和清晰秩序的语境里");
            case "LOU_XIU" -> new StarLineage("西方白虎七宿第二宿", "收束、统筹、仓储和守成", "娄带聚合、收纳和把资源拢住的意味");
            case "MAO_XIU" -> new StarLineage("西方白虎七宿第四宿", "清醒、分辨、判断和锋利边界", "昴有聚星成团的意象，适合做清晰辨认的锚点");
            case "NIU_XIU" -> new StarLineage("北方玄武七宿第二宿", "冬水、耐力、承接和蓄势", "牛带稳定、耐负和慢慢积攒力气的意味");
            case "XU_XIU" -> new StarLineage("北方玄武七宿第四宿", "冬水、幽静、收敛、藏蓄和留白", "虚字本身带空处、容纳、未满和深处仍有余地的意味");
            case "WEI_XIU" -> new StarLineage("北方玄武七宿第五宿", "水边的警觉、临界、适应和风险感知", "危在这里不是坏结论，而是提醒人看见边界、变化和需要谨慎的位置");
            default -> new StarLineage("传统二十八宿体系", "可记忆、可归类的星象名称", "它在这里作为传统名称锚点使用");
        };
    }

    record StarLineage(String group, String imagery, String traits) {
    }
}
