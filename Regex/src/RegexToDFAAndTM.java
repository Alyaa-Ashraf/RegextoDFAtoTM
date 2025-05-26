import java.util.*;
import java.util.stream.Collectors;

// --- Part 1: Regex to NFA ---

/**
 * NFATransition represents a transition in the NFA.
 * 'symbol' is null for an epsilon transition.
 */
class NFATransition {
    int fromState;
    Character symbol; // null for epsilon
    int toState;

    public NFATransition(int fromState, Character symbol, int toState) {
        this.fromState = fromState;
        this.symbol = symbol;
        this.toState = toState;
    }

    @Override
    public String toString() {
        return fromState + " --" + (symbol == null ? "ε" : symbol) + "--> " + toState;
    }
}

/**
 * NFA represents a Nondeterministic Finite Automaton.
 */
class NFA {
    static int nextStateId = 0; // Global counter for unique state IDs

    int startState;
    int acceptState;
    Set<Integer> allStates;
    List<NFATransition> transitions;
    Set<Character> alphabet;

    public NFA(int startState, int acceptState) {
        this.startState = startState;
        this.acceptState = acceptState;
        this.allStates = new HashSet<>(Arrays.asList(startState, acceptState));
        this.transitions = new ArrayList<>();
        this.alphabet = new HashSet<>();
    }

    public static void resetStateIdCounter() {
        nextStateId = 0;
    }

    public static int getNewStateId() {
        return nextStateId++;
    }

    public void addTransition(NFATransition transition) {
        this.transitions.add(transition);
        this.allStates.add(transition.fromState);
        this.allStates.add(transition.toState);
        if (transition.symbol != null) {
            this.alphabet.add(transition.symbol);
        }
    }

    public void addAllTransitions(Collection<NFATransition> transitions) {
        transitions.forEach(this::addTransition);
    }

    public void addAllStates(Collection<Integer> states) {
        this.allStates.addAll(states);
    }

    public void addAllAlphabetSymbols(Collection<Character> symbols) {
        symbols.stream().filter(Objects::nonNull).forEach(this.alphabet::add);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("NFA:\n");
        sb.append("Start State: ").append(startState).append("\n");
        sb.append("Accept State: ").append(acceptState).append("\n");
        sb.append("States: ").append(allStates).append("\n");
        sb.append("Alphabet: ").append(alphabet).append("\n");
        sb.append("Transitions:\n");
        for (NFATransition t : transitions) {
            sb.append("  ").append(t).append("\n");
        }
        return sb.toString();
    }
}

/**
 * NFABuilder constructs an NFA from a simplified regex.
 * Supports: concatenation (ab), alternation (a|b), Kleene star (a*),
 * one or more (a+), zero or one (a?).
 * Precedence: *, +, ? (highest), then concatenation, then | (lowest). No parentheses.
 */
class NFABuilder {

    private static NFA createLiteralNFA(char literal) {
        int start = NFA.getNewStateId();
        int end = NFA.getNewStateId();
        NFA nfa = new NFA(start, end);
        nfa.addTransition(new NFATransition(start, literal, end));
        nfa.alphabet.add(literal);
        return nfa;
    }

    private static NFA createStarNFA(NFA nfaFragment) {
        int newStart = NFA.getNewStateId();
        int newAccept = NFA.getNewStateId();
        NFA starNFA = new NFA(newStart, newAccept);
        starNFA.addAllStates(nfaFragment.allStates);
        starNFA.addAllTransitions(nfaFragment.transitions);
        starNFA.addAllAlphabetSymbols(nfaFragment.alphabet);
        starNFA.addTransition(new NFATransition(newStart, null, nfaFragment.startState));
        starNFA.addTransition(new NFATransition(nfaFragment.acceptState, null, newAccept));
        starNFA.addTransition(new NFATransition(nfaFragment.acceptState, null, nfaFragment.startState));
        starNFA.addTransition(new NFATransition(newStart, null, newAccept));
        return starNFA;
    }

    private static NFA createPlusNFA(NFA nfaFragment) {
        int newStart = NFA.getNewStateId();
        int newAccept = NFA.getNewStateId();
        NFA plusNFA = new NFA(newStart, newAccept);
        plusNFA.addAllStates(nfaFragment.allStates);
        plusNFA.addAllTransitions(nfaFragment.transitions);
        plusNFA.addAllAlphabetSymbols(nfaFragment.alphabet);
        plusNFA.addTransition(new NFATransition(newStart, null, nfaFragment.startState));
        plusNFA.addTransition(new NFATransition(nfaFragment.acceptState, null, newAccept));
        plusNFA.addTransition(new NFATransition(nfaFragment.acceptState, null, nfaFragment.startState));
        return plusNFA;
    }

    private static NFA createOptionalNFA(NFA nfaFragment) {
        int newStart = NFA.getNewStateId();
        int newAccept = NFA.getNewStateId();
        NFA optionalNFA = new NFA(newStart, newAccept);
        optionalNFA.addAllStates(nfaFragment.allStates);
        optionalNFA.addAllTransitions(nfaFragment.transitions);
        optionalNFA.addAllAlphabetSymbols(nfaFragment.alphabet);
        optionalNFA.addTransition(new NFATransition(newStart, null, nfaFragment.startState));
        optionalNFA.addTransition(new NFATransition(nfaFragment.acceptState, null, newAccept));
        optionalNFA.addTransition(new NFATransition(newStart, null, newAccept));
        return optionalNFA;
    }

    private static NFA concatenateNFA(NFA nfa1, NFA nfa2) {
        NFA concatNFA = new NFA(nfa1.startState, nfa2.acceptState);
        concatNFA.addAllStates(nfa1.allStates);
        concatNFA.addAllStates(nfa2.allStates);
        concatNFA.addAllTransitions(nfa1.transitions);
        concatNFA.addAllTransitions(nfa2.transitions);
        concatNFA.addAllAlphabetSymbols(nfa1.alphabet);
        concatNFA.addAllAlphabetSymbols(nfa2.alphabet);
        concatNFA.addTransition(new NFATransition(nfa1.acceptState, null, nfa2.startState));
        return concatNFA;
    }

    private static NFA alternateNFA(NFA nfa1, NFA nfa2) {
        int newStart = NFA.getNewStateId();
        int newAccept = NFA.getNewStateId();
        NFA altNFA = new NFA(newStart, newAccept);
        altNFA.addAllStates(nfa1.allStates);
        altNFA.addAllStates(nfa2.allStates);
        altNFA.addAllTransitions(nfa1.transitions);
        altNFA.addAllTransitions(nfa2.transitions);
        altNFA.addAllAlphabetSymbols(nfa1.alphabet);
        altNFA.addAllAlphabetSymbols(nfa2.alphabet);
        altNFA.addTransition(new NFATransition(newStart, null, nfa1.startState));
        altNFA.addTransition(new NFATransition(newStart, null, nfa2.startState));
        altNFA.addTransition(new NFATransition(nfa1.acceptState, null, newAccept));
        altNFA.addTransition(new NFATransition(nfa2.acceptState, null, newAccept));
        return altNFA;
    }

    private static NFA parseUnit(String unitStr) {
        if (unitStr.length() == 1) return createLiteralNFA(unitStr.charAt(0));
        if (unitStr.length() == 2) {
            NFA literalNFA = createLiteralNFA(unitStr.charAt(0));
            switch (unitStr.charAt(1)) {
                case '*': return createStarNFA(literalNFA);
                case '+': return createPlusNFA(literalNFA);
                case '?': return createOptionalNFA(literalNFA);
                default: throw new IllegalArgumentException("Invalid operator in unit: " + unitStr);
            }
        }
        throw new IllegalArgumentException("Invalid unit: " + unitStr);
    }

    private static NFA parseConcatenationSequence(String sequenceStr) {
        if (sequenceStr.isEmpty()) {
            int s = NFA.getNewStateId();
            return new NFA(s, s); // Epsilon NFA
        }
        List<NFA> unitNFAs = new ArrayList<>();
        int i = 0;
        while (i < sequenceStr.length()) {
            if (i + 1 < sequenceStr.length() && Set.of('*', '+', '?').contains(sequenceStr.charAt(i + 1))) {
                unitNFAs.add(parseUnit(sequenceStr.substring(i, i + 2)));
                i += 2;
            } else {
                unitNFAs.add(parseUnit(sequenceStr.substring(i, i + 1)));
                i += 1;
            }
        }
        if (unitNFAs.isEmpty()) { // Should be unreachable if sequenceStr not empty
            int s = NFA.getNewStateId(); return new NFA(s,s);
        }
        NFA resultNFA = unitNFAs.get(0);
        for (int j = 1; j < unitNFAs.size(); j++) {
            resultNFA = concatenateNFA(resultNFA, unitNFAs.get(j));
        }
        return resultNFA;
    }

    public static NFA buildFromRegex(String regex) {
        NFA.resetStateIdCounter();
        if (regex == null || regex.isEmpty()) {
            int s = NFA.getNewStateId();
            return new NFA(s, s); // NFA for empty string
        }
        String[] alternationParts = regex.split("\\|");
        List<NFA> alternationNFAs = new ArrayList<>();
        for (String part : alternationParts) {
            if (part.isEmpty() && alternationParts.length > 1) {
                int s = NFA.getNewStateId();
                alternationNFAs.add(new NFA(s, s)); // Epsilon choice in alternation
            } else {
                alternationNFAs.add(parseConcatenationSequence(part));
            }
        }
        if (alternationNFAs.isEmpty()) { // Should be unreachable
            int s = NFA.getNewStateId(); return new NFA(s,s);
        }
        NFA finalNFA = alternationNFAs.get(0);
        for (int i = 1; i < alternationNFAs.size(); i++) {
            finalNFA = alternateNFA(finalNFA, alternationNFAs.get(i));
        }
        return finalNFA;
    }
}

// --- Part 2: NFA to DFA ---

/**
 * DFA represents a Deterministic Finite Automaton.
 */
class DFA {
    static int nextDfaStateId = 0;
    int startStateId = -1;
    Set<Integer> acceptStateIds = new HashSet<>();
    Map<Integer, Map<Character, Integer>> transitions = new HashMap<>();
    Set<Character> alphabet;
    Map<Integer, Set<Integer>> dfaStateToNfaStatesMap = new HashMap<>();

    public DFA(Set<Character> alphabet) {
        this.alphabet = alphabet;
    }

    public static void resetDfaStateIdCounter() { nextDfaStateId = 0; }
    public static int getNewDfaStateId() { return nextDfaStateId++; }

    public void addTransition(int fromDfaState, char symbol, int toDfaState) {
        transitions.computeIfAbsent(fromDfaState, k -> new HashMap<>()).put(symbol, toDfaState);
    }

    public void setDfaStateMapping(int dfaId, Set<Integer> nfaStates) {
        dfaStateToNfaStatesMap.put(dfaId, nfaStates);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("DFA:\n");
        sb.append("Alphabet: ").append(alphabet).append("\n");
        sb.append("Start State ID: ").append(startStateId).append(" (NFA states: ").append(dfaStateToNfaStatesMap.get(startStateId)).append(")\n");
        String acceptStatesStr = acceptStateIds.stream()
                .map(id -> id + " (NFA states: " + dfaStateToNfaStatesMap.get(id) + ")")
                .collect(Collectors.joining(", ", "[", "]"));
        sb.append("Accept State IDs: ").append(acceptStatesStr).append("\n");
        sb.append("Transitions:\n");
        transitions.forEach((fromDfaState, transMap) ->
                transMap.forEach((symbol, toDfaState) ->
                        sb.append("  (").append(fromDfaState).append(" (").append(dfaStateToNfaStatesMap.get(fromDfaState)).append(")")
                                .append(", ").append(symbol)
                                .append(") -> ").append(toDfaState).append(" (").append(dfaStateToNfaStatesMap.get(toDfaState)).append(")\n")
                )
        );
        return sb.toString();
    }
}

/**
 * DFAConverter converts an NFA to a DFA using subset construction.
 */
class DFAConverter {
    private static Set<Integer> epsilonClosure(Set<Integer> nfaStates, NFA nfa) {
        Set<Integer> closure = new HashSet<>(nfaStates);
        Stack<Integer> stack = new Stack<>();
        stack.addAll(nfaStates);
        while (!stack.isEmpty()) {
            int currentState = stack.pop();
            for (NFATransition t : nfa.transitions) {
                if (t.fromState == currentState && t.symbol == null && closure.add(t.toState)) {
                    stack.push(t.toState);
                }
            }
        }
        return closure;
    }

    private static Set<Integer> move(Set<Integer> nfaStates, char symbol, NFA nfa) {
        return nfaStates.stream()
                .flatMap(nfaState -> nfa.transitions.stream()
                        .filter(t -> t.fromState == nfaState && t.symbol != null && t.symbol.equals(symbol))
                        .map(t -> t.toState))
                .collect(Collectors.toSet());
    }

    public static DFA convert(NFA nfa) {
        DFA.resetDfaStateIdCounter();
        DFA dfa = new DFA(nfa.alphabet);
        Map<Set<Integer>, Integer> dfaStatesMap = new HashMap<>();
        Queue<Set<Integer>> unprocessedDfaStates = new LinkedList<>();

        Set<Integer> initialNfaStates = epsilonClosure(Collections.singleton(nfa.startState), nfa);
        int initialDfaStateId = DFA.getNewDfaStateId();
        dfaStatesMap.put(initialNfaStates, initialDfaStateId);
        dfa.setDfaStateMapping(initialDfaStateId, initialNfaStates);
        unprocessedDfaStates.add(initialNfaStates);
        dfa.startStateId = initialDfaStateId;
        if (initialNfaStates.contains(nfa.acceptState)) {
            dfa.acceptStateIds.add(initialDfaStateId);
        }

        while (!unprocessedDfaStates.isEmpty()) {
            Set<Integer> currentNfaStatesSet = unprocessedDfaStates.poll();
            int currentDfaStateId = dfaStatesMap.get(currentNfaStatesSet);

            for (char symbol : nfa.alphabet) {
                Set<Integer> moveResult = move(currentNfaStatesSet, symbol, nfa);
                if (moveResult.isEmpty()) continue;
                Set<Integer> targetNfaStatesSet = epsilonClosure(moveResult, nfa);
                int targetDfaStateId;
                if (!dfaStatesMap.containsKey(targetNfaStatesSet)) {
                    targetDfaStateId = DFA.getNewDfaStateId();
                    dfaStatesMap.put(targetNfaStatesSet, targetDfaStateId);
                    dfa.setDfaStateMapping(targetDfaStateId, targetNfaStatesSet);
                    unprocessedDfaStates.add(targetNfaStatesSet);
                    if (targetNfaStatesSet.contains(nfa.acceptState)) {
                        dfa.acceptStateIds.add(targetDfaStateId);
                    }
                } else {
                    targetDfaStateId = dfaStatesMap.get(targetNfaStatesSet);
                }
                dfa.addTransition(currentDfaStateId, symbol, targetDfaStateId);
            }
        }
        return dfa;
    }
}

// --- Part 3: DFA to Turing Machine ---

/**
 * TMRule represents a transition rule for the Turing Machine.
 */
class TMRule {
    String nextState;
    char symbolToWrite;
    char moveDirection; // 'L', 'R', 'S'

    public TMRule(String nextState, char symbolToWrite, char moveDirection) {
        this.nextState = nextState;
        this.symbolToWrite = symbolToWrite;
        this.moveDirection = moveDirection;
    }

    @Override
    public String toString() {
        return "(to_state=" + nextState + ", write=" + symbolToWrite + ", move=" + moveDirection + ")";
    }
}

/**
 * TuringMachine simulates a DFA.
 */
class TuringMachine {
    private static final String TM_ACCEPT_STATE = "q_accept_tm";
    private static final String TM_REJECT_STATE = "q_reject_tm";
    private static final char BLANK_SYMBOL = '_';

    private Set<String> tmStates = new HashSet<>(Arrays.asList(TM_ACCEPT_STATE, TM_REJECT_STATE));
    private String tmStartState;
    private Set<Character> tapeAlphabet = new HashSet<>(Collections.singleton(BLANK_SYMBOL));
    private Map<String, Map<Character, TMRule>> tmTransitions = new HashMap<>();

    private String dfaStateToTmState(int dfaStateId) { return "q_dfa_" + dfaStateId; }

    public static TuringMachine fromDFA(DFA dfa) {
        TuringMachine tm = new TuringMachine();
        tm.tapeAlphabet.addAll(dfa.alphabet);

        if (dfa.startStateId == -1 && dfa.alphabet.isEmpty() && dfa.dfaStateToNfaStatesMap.isEmpty()) {
            // Handle case of truly empty DFA from perhaps an invalid regex or very specific empty NFA
            // For example, regex "" might result in NFA start=accept, which becomes DFA start=accept
            // If dfa.startStateId is -1, it means the DFA construction didn't even create a start state.
            // This might imply the NFA was empty or couldn't be processed.
            // A minimal TM might just reject everything or accept empty string if NFA did.
            // For now, if DFA start is -1, TM start will be null, and simulate() handles it.
        } else if (dfa.startStateId != -1) {
            tm.tmStartState = tm.dfaStateToTmState(dfa.startStateId);
            tm.tmStates.add(tm.tmStartState);
        }


        // Convert DFA transitions to TM transitions
        dfa.transitions.forEach((fromDfaState, transMap) -> {
            String currentTmState = tm.dfaStateToTmState(fromDfaState);
            tm.tmStates.add(currentTmState);
            transMap.forEach((symbol, toDfaState) -> {
                String nextTmState = tm.dfaStateToTmState(toDfaState);
                tm.tmStates.add(nextTmState);
                tm.tmTransitions.computeIfAbsent(currentTmState, k -> new HashMap<>())
                        .put(symbol, new TMRule(nextTmState, symbol, 'R'));
            });
        });

        // Collect all unique DFA states involved for adding default TM rules
        Set<Integer> allRelevantDfaStates = new HashSet<>();
        if (dfa.startStateId != -1) allRelevantDfaStates.add(dfa.startStateId);
        allRelevantDfaStates.addAll(dfa.acceptStateIds);
        dfa.transitions.forEach((from, transMap) -> {
            allRelevantDfaStates.add(from);
            allRelevantDfaStates.addAll(transMap.values());
        });
        // Ensure all accept states are added to tmStates for processing below
        dfa.acceptStateIds.forEach(id -> tm.tmStates.add(tm.dfaStateToTmState(id)));


        // Add rules for BLANK_SYMBOL and symbols not in DFA transitions (implicit rejection)
        for (int dfaStateId : allRelevantDfaStates) {
            String currentTmState = tm.dfaStateToTmState(dfaStateId);
            Map<Character, TMRule> stateSpecificRules = tm.tmTransitions.computeIfAbsent(currentTmState, k -> new HashMap<>());

            String nextStateOnBlank = dfa.acceptStateIds.contains(dfaStateId) ? TM_ACCEPT_STATE : TM_REJECT_STATE;
            stateSpecificRules.put(BLANK_SYMBOL, new TMRule(nextStateOnBlank, BLANK_SYMBOL, 'S'));

            for (char sym : dfa.alphabet) {
                if (!dfa.transitions.getOrDefault(dfaStateId, Collections.emptyMap()).containsKey(sym)) {
                    stateSpecificRules.putIfAbsent(sym, new TMRule(TM_REJECT_STATE, sym, 'S'));
                }
            }
        }

        // Handle case: DFA has a start state that's an accept state, but no transitions (e.g. regex for empty string "")
        if (dfa.startStateId != -1 && dfa.transitions.isEmpty() && dfa.acceptStateIds.contains(dfa.startStateId)) {
            String startTmState = tm.dfaStateToTmState(dfa.startStateId);
            Map<Character, TMRule> rules = tm.tmTransitions.computeIfAbsent(startTmState, k -> new HashMap<>());
            rules.put(BLANK_SYMBOL, new TMRule(TM_ACCEPT_STATE, BLANK_SYMBOL, 'S'));
            for (char sym : dfa.alphabet) { // Any actual symbol should lead to reject for empty string matcher
                rules.putIfAbsent(sym, new TMRule(TM_REJECT_STATE, sym, 'S'));
            }
        }
        return tm;
    }

    public boolean simulate(String input) {
        if (this.tmStartState == null) {
            // This can happen if the DFA was effectively empty or invalid.
            // If the regex was empty "", the DFA should accept empty string.
            // The fromDFA logic for empty string regex (NFA start=accept) should create a DFA
            // where startStateId is an acceptStateId, and it has a BLANK transition to TM_ACCEPT_STATE.
            // If tmStartState is null here, it implies a more fundamental issue or an unhandled DFA structure.
            // For an empty regex "", the NFA is start=accept. DFA is start=accept.
            // TM should accept "" (blank on tape from start).
            // Let's assume if tmStartState is null, it means no valid TM could be formed to accept anything.
            // However, an empty regex "" should result in a TM that accepts an empty input.
            // This check might need refinement based on how fromDFA handles truly empty/minimal DFAs.
            // If the DFA had a start state 0, and it was an accept state, and no transitions,
            // then tmStartState would be q_dfa_0. Reading BLANK should go to TM_ACCEPT_STATE.
            // So, if tmStartState is null, it means the DFA itself was problematic.
            System.err.println("TM start state is not initialized. Defaulting to reject. Input: \"" + input + "\"");
            return false;
        }

        List<Character> tape = input.chars().mapToObj(c -> (char) c).collect(Collectors.toList());
        String currentState = this.tmStartState;
        int headPosition = 0;
        int maxSteps = input.length() + 5; // Prevent infinite loops

        for (int steps = 0; steps < maxSteps; steps++) {
            if (TM_ACCEPT_STATE.equals(currentState)) return true;
            if (TM_REJECT_STATE.equals(currentState)) return false;

            char charUnderHead = (headPosition >= 0 && headPosition < tape.size()) ? tape.get(headPosition) : BLANK_SYMBOL;

            Map<Character, TMRule> stateTransitionsMap = tmTransitions.get(currentState);
            if (stateTransitionsMap == null) return false; // No transitions from state

            TMRule rule = stateTransitionsMap.get(charUnderHead);
            if (rule == null) return false; // No rule for this symbol

            if (headPosition >= 0 && headPosition < tape.size()) {
                tape.set(headPosition, rule.symbolToWrite);
            } else if (headPosition == tape.size() && rule.symbolToWrite != BLANK_SYMBOL) {
                tape.add(rule.symbolToWrite);
            }

            currentState = rule.nextState;
            if (rule.moveDirection == 'R') headPosition++;
            else if (rule.moveDirection == 'L') headPosition = Math.max(0, headPosition - 1);
        }
        return false; // Max steps reached
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Turing Machine (simulating DFA):\n");
        sb.append("Tape Alphabet: ").append(tapeAlphabet).append("\n");
        sb.append("Blank Symbol: '").append(BLANK_SYMBOL).append("'\n");
        sb.append("States: ").append(tmStates).append("\n");
        sb.append("Start State: ").append(tmStartState).append("\n");
        sb.append("Accept State: ").append(TM_ACCEPT_STATE).append("\n");
        sb.append("Reject State: ").append(TM_REJECT_STATE).append("\n");
        sb.append("Transitions:\n");
        tmTransitions.forEach((fromState, transMap) ->
                transMap.forEach((readSymbol, rule) ->
                        sb.append("  (").append(fromState).append(", '").append(readSymbol).append("') -> ")
                                .append(rule).append("\n")
                )
        );
        return sb.toString();
    }
}

// --- Part 4: Main Class for Demonstration ---
public class RegexToDFAAndTM {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter a simplified regular expression (e.g., a*b, a|b, ab, a+c, d?e):");
        String regex = scanner.nextLine();

        System.out.println("\nBuilding NFA for regex: \"" + regex + "\"");
        NFA nfa = NFABuilder.buildFromRegex(regex);
        System.out.println(nfa);

        System.out.println("\nConverting NFA to DFA...");
        DFA dfa = DFAConverter.convert(nfa);
        System.out.println(dfa);

        System.out.println("\nConverting DFA to Turing Machine representation...");
        TuringMachine tm = TuringMachine.fromDFA(dfa);
        System.out.println(tm);

        System.out.println("\nEnter a string to test with the Turing Machine (or type 'exit'):");
        String inputString;
        while (!(inputString = scanner.nextLine()).equalsIgnoreCase("exit")) {
            if (inputString == null) break; // Should not happen with scanner.nextLine()
            System.out.println("Simulating TM on input: \"" + inputString + "\"");
            boolean accepted = tm.simulate(inputString);
            System.out.println("Input \"" + inputString + "\" accepted by TM: " + accepted);
            System.out.println("\nEnter another string or 'exit':");
        }
        scanner.close();
        System.out.println("Exiting.");
    }
}
