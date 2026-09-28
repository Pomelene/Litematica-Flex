package io.github.litematicaflex.runtime;

import java.util.*;
import java.util.function.BiPredicate;

/** Allocates each available item at most once, preferring exact items before substitutes. */
public final class MaterialPlanner {
    public record Demand(String id,int count) {}
    public record Supply(String id,int count) {}
    public record Row(String expected,int needed,int covered,Map<String,Integer> assigned) {}
    private MaterialPlanner() {}

    public static List<Row> plan(List<Demand> demands,List<Supply> supplies,BiPredicate<String,String> accepted) {
        Map<String,Integer> remaining=new LinkedHashMap<>();
        for(var supply:supplies)if(supply.count()>0)remaining.merge(supply.id(),supply.count(),Integer::sum);
        List<Map<String,Integer>> assignments=new ArrayList<>();
        for(var demand:demands)assignments.add(new LinkedHashMap<>());
        for(int i=0;i<demands.size();i++)take(demands.get(i).id(),demands.get(i).count(),remaining,assignments.get(i));
        for(int i=0;i<demands.size();i++) {
            var demand=demands.get(i);var assigned=assignments.get(i);
            int missing=Math.max(0,demand.count()-assigned.values().stream().mapToInt(Integer::intValue).sum());
            for(String candidate:new ArrayList<>(remaining.keySet())) {
                if(missing==0)break;
                if(!candidate.equals(demand.id()) && accepted.test(demand.id(),candidate))
                    missing-=take(candidate,missing,remaining,assigned);
            }
        }
        List<Row> rows=new ArrayList<>();
        for(int i=0;i<demands.size();i++) {
            var d=demands.get(i);var assigned=assignments.get(i);
            rows.add(new Row(d.id(),d.count(),assigned.values().stream().mapToInt(Integer::intValue).sum(),Map.copyOf(assigned)));
        }
        return rows;
    }

    private static int take(String id,int limit,Map<String,Integer> remaining,Map<String,Integer> assigned) {
        int count=Math.min(Math.max(0,limit),remaining.getOrDefault(id,0));
        if(count>0){remaining.put(id,remaining.get(id)-count);assigned.merge(id,count,Integer::sum);}
        return count;
    }
}
