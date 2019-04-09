package core.global;

import com.google.common.collect.ImmutableSet;
import core.Literal;

import java.util.*;
import java.util.stream.Stream;

public class RuleMap {

    private final Map<Literal, Set<ImmutableSet<Literal>>> map;
    private int ruleNumber;

    public RuleMap() {
        this.map = new HashMap<>();
        this.ruleNumber = 0;
    }

    public RuleMap(Map<Literal, Set<ImmutableSet<Literal>>> map) {
        this.map = map;
    }

    public Set<ImmutableSet<Literal>> getRuleSet(Literal literal){
        return map.get(literal);
    }

    public void addRule(Literal head, ImmutableSet<Literal> body){
        Set<ImmutableSet<Literal>> bodies = map.get(head);
        if(bodies == null){
            Set<ImmutableSet<Literal>> s = new HashSet<>();
            s.add(body);
            map.put(head, s);
            ruleNumber++;
        }else {
            if(bodies.add(body)){
                ruleNumber++;
            }
        }
    }

    public void addRuleSet(Literal head, Set<ImmutableSet<Literal>> body){
            map.put(head, body);
    }

    // Returns true if there is no more rule body with head "head" after deletion
    // also deletes the map entry for the head
//    public boolean deleteRule(Literal head, RuleBody body){
//        Set<RuleBody> bodies = map.get(head);
//        if(bodies != null){
//            bodies.remove(body);
//            if(bodies.isEmpty()){
//                map.remove(head);
//                return true;
//            }
//            return false;
//        }
//        return true;
//    }

    public void remove(Literal a){
        map.remove(a);
    }

    public void replace(Literal head, Set<ImmutableSet<Literal>> bodies){
        map.replace(head, bodies);
    }


    public Stream<Literal> getAllBodyAtoms(){
        return map.values().stream()
                .flatMap(s -> s.stream())
                .flatMap(s -> s.stream());
    }

    public Set<Map.Entry<Literal, Set<ImmutableSet<Literal>>>> entrySet(){
        return map.entrySet();
    }
    public Set<Literal> keySet(){
        return map.keySet();
    }
    public Collection<Set<ImmutableSet<Literal>>> values(){
        return map.values();
    }

    public int getRuleNumber() {
        return ruleNumber;
    }
}
