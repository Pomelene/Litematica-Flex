package io.github.litematicaflex.runtime;

import java.util.*;
import java.util.function.BiPredicate;

/** Groups only mutually compatible demands; overlapping groups never become transitive. */
public final class MaterialGroupPlanner {
    public record Demand(String id,int total,int missing,int mismatched) {}
    public record Group(List<String> members,int total,int missing,int mismatched,int covered,Map<String,Integer> assigned) {}
    private MaterialGroupPlanner() {}

    public static List<Group> plan(List<Demand> demands,List<MaterialPlanner.Supply> supplies,BiPredicate<String,String> accepts) {
        var needs=demands.stream().map(d -> new MaterialPlanner.Demand(d.id(),d.missing())).toList();
        var allocations=MaterialPlanner.plan(needs,supplies,accepts);
        var groups=new ArrayList<List<Integer>>();
        for(int i=0;i<demands.size();i++) {
            Demand candidate=demands.get(i);
            List<Integer> destination=null;
            for(var group:groups) {
                if(group.stream().allMatch(index -> {
                    String member=demands.get(index).id();
                    return member.equals(candidate.id()) ||
                        (accepts.test(member,candidate.id()) && accepts.test(candidate.id(),member));
                })) {destination=group;break;}
            }
            if(destination==null){destination=new ArrayList<>();groups.add(destination);}
            destination.add(i);
        }
        var result=new ArrayList<Group>();
        for(var group:groups) {
            var members=new ArrayList<String>();
            var assigned=new LinkedHashMap<String,Integer>();
            int total=0,missing=0,mismatched=0,covered=0;
            for(int index:group) {
                Demand demand=demands.get(index);
                MaterialPlanner.Row allocation=allocations.get(index);
                members.add(demand.id());
                total+=demand.total();missing+=demand.missing();mismatched+=demand.mismatched();covered+=allocation.covered();
                allocation.assigned().forEach((id,count)->assigned.merge(id,count,Integer::sum));
            }
            result.add(new Group(List.copyOf(members),total,missing,mismatched,covered,
                Collections.unmodifiableMap(new LinkedHashMap<>(assigned))));
        }
        return List.copyOf(result);
    }
}
