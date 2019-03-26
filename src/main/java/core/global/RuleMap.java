package core.global;

import com.google.common.collect.Sets;
import core.Atom;
import core.RuleBody;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class RuleMap {

    private final Map<Atom, Set<RuleBody>> map;

    public RuleMap() {
        this.map = new HashMap<>();
    }

    public RuleMap(Map<Atom, Set<RuleBody>> map) {
        this.map = map;
    }

    public Set<RuleBody> getRuleSet(Atom atom){
        return map.get(atom);
    }

    public void addRule(Atom head, RuleBody body){
        Set<RuleBody> bodies = map.get(head);
        if(bodies == null){
            map.put(head, Sets.newHashSet(body));
        }else {
            bodies.add(body);
        }
    }

    public void addRuleSet(Atom head, Set<RuleBody> body){
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

    public void replace(Atom head, Set<RuleBody> bodies){
        map.replace(head, bodies);
    }


    public Stream<Atom> getAllBodyAtoms(){
        return map.values().stream()
                .flatMap(s -> s.stream())
                .flatMap(r -> r.getPositiveAndNegatedAtoms().stream());
    }

    public Set<Map.Entry<Atom, Set<RuleBody>>> entrySet(){
        return map.entrySet();
    }
    public Set<Atom> keySet(){
        return map.keySet();
    }
    public Collection<Set<RuleBody>> values(){
        return map.values();
    }
}
