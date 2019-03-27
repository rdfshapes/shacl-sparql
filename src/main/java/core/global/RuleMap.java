package core.global;

import com.google.common.collect.ImmutableSet;
import core.Atom;

import java.util.*;
import java.util.stream.Stream;

public class RuleMap {

    private final Map<Atom, Set<ImmutableSet<Atom>>> map;
    private int ruleNumber;

    public RuleMap() {
        this.map = new HashMap<>();
        this.ruleNumber = 0;
    }

    public RuleMap(Map<Atom, Set<ImmutableSet<Atom>>> map) {
        this.map = map;
    }

    public Set<ImmutableSet<Atom>> getRuleSet(Atom atom){
        return map.get(atom);
    }

    public void addRule(Atom head, ImmutableSet<Atom> body){
        Set<ImmutableSet<Atom>> bodies = map.get(head);
        if(bodies == null){
            Set<ImmutableSet<Atom>> s = new HashSet<>();
            s.add(body);
            map.put(head, s);
            ruleNumber++;
        }else {
            if(bodies.add(body)){
                ruleNumber++;
            }
        }
    }

    public void addRuleSet(Atom head, Set<ImmutableSet<Atom>> body){
            map.put(head, body);
    }

    // Returns true if there is no more rule body with head "head" after deletion
    // also deletes the map entry for the head
//    public boolean deleteRule(Atom head, RuleBody body){
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

    public void remove(Atom a){
        map.remove(a);
    }

    public void replace(Atom head, Set<ImmutableSet<Atom>> bodies){
        map.replace(head, bodies);
    }


    public Stream<Atom> getAllBodyAtoms(){
        return map.values().stream()
                .flatMap(s -> s.stream())
                .flatMap(s -> s.stream());
    }

    public Set<Map.Entry<Atom, Set<ImmutableSet<Atom>>>> entrySet(){
        return map.entrySet();
    }
    public Set<Atom> keySet(){
        return map.keySet();
    }
    public Collection<Set<ImmutableSet<Atom>>> values(){
        return map.values();
    }

    public int getRuleNumber() {
        return ruleNumber;
    }
}
