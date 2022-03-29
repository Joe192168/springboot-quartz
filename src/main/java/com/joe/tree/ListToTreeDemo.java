package com.joe.tree;

import com.alibaba.fastjson.JSON;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.CollectionUtils;
 
import java.util.*;
import java.util.stream.Collectors;
 
/**
 * 平行集合转Tree集合测试示例
 */
public class ListToTreeDemo {
 
    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        List<DemoData> demoData = transToTree(data());
        long end = System.currentTimeMillis();
        System.out.println("耗时："+ (end - start) + "ms，转换后的数据：\n" + JSON.toJSONString(demoData));
    }
 
    /**
     * 将数据转换为树型结构
     *
     * @param sources sources
     * @return {@link List<DemoData>}
     */
    public static List<DemoData> transToTree(List<DemoData> sources) {
        if (CollectionUtils.isEmpty(sources)) {
            return Collections.emptyList();
        }
        Map<Integer, DemoData> sourceMap = sources.stream().collect(Collectors.toMap(DemoData::getId, e -> e));
        Map<Integer, List<DemoData>> pIdToChildrenListMap = sources.stream().collect(Collectors.groupingBy(DemoData::getPid));
        List<Integer> willBeRemovedIdList = new LinkedList<>();
        for (Map.Entry<Integer, List<DemoData>> entry : pIdToChildrenListMap.entrySet()) {
            DemoData demoData = sourceMap.get(entry.getKey());
            if (demoData == null) {
                continue;
            }
            demoData.setChildren(entry.getValue().stream().sorted(Comparator.comparing(DemoData::getSort)).collect(Collectors.toList()));
            willBeRemovedIdList.add(entry.getKey());
        }
        willBeRemovedIdList.forEach(pIdToChildrenListMap::remove);
        // 获取顶级
        return pIdToChildrenListMap.values().stream().flatMap(Collection::stream).sorted(Comparator.comparing(DemoData::getSort)).collect(Collectors.toList());
    }
 
    /**
     * 生成测试数据
     *
     * @return {@link List<DemoData>}
     */
    public static List<DemoData> data() {
        List<DemoData> results = new LinkedList<>();
        results.add(new DemoData(1, 0, "CE_SHI_A", "测试A", 1));
        results.add(new DemoData(2, 1, "CE_SHI_A_001", "测试A001", 2));
        results.add(new DemoData(3, 1, "CE_SHI_A_002", "测试A002", 3));
        results.add(new DemoData(4, 1, "CE_SHI_A_003", "测试A003", 4));
 
        results.add(new DemoData(5, 0, "CE_SHI_B", "测试B", 5));
        results.add(new DemoData(6, 5, "CE_SHI_B_001", "测试B001", 6));
        results.add(new DemoData(7, 5, "CE_SHI_B_002", "测试B002", 7));
        results.add(new DemoData(8, 5, "CE_SHI_B_003", "测试B003", 8));
 
        results.add(new DemoData(9, 2, "CE_SHI_A01", "测试A01", 9));
        results.add(new DemoData(10, 2, "CE_SHI_A02_001", "测试A02001", 10));
        results.add(new DemoData(11, 2, "CE_SHI_A03_002", "测试A03002", 11));
        results.add(new DemoData(12, 2, "CE_SHI_A04_003", "测试A04003", 12));
 
        results.add(new DemoData(13, 6, "CE_SHI_B01", "测试B01", 13));
        results.add(new DemoData(14, 6, "CE_SHI_B02_001", "测试B02001", 14));
        results.add(new DemoData(15, 6, "CE_SHI_B03_002", "测试03B002", 15));
        results.add(new DemoData(16, 6, "CE_SHI_B04_003", "测试B04003", 16));
        return results;
    }
 
}
 
@Data
@AllArgsConstructor
@NoArgsConstructor
class DemoData {
    /**
     * ID
     */
    private Integer id;
    /**
     * 父级ID
     */
    private Integer pid;
    /**
     * 编码
     */
    private String code;
    /**
     * 名字
     */
    private String name;
    /**
     * 排序编码
     */
    private Integer sort;
    /**
     * 子节点数据
     */
    private List<DemoData> children;
 
    public DemoData(Integer id, Integer pid, String code, String name, Integer sort) {
        this.id = id;
        this.pid = pid;
        this.code = code;
        this.name = name;
        this.sort = sort;
    }
}
 