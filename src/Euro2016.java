import java.util.Date;
import java.util.HashSet;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import sync.*;

public class Euro2016 {



    public static void init() {

    }


    static class FanA extends TemplateThread {

        public FanA(int numRuns) {
            super(numRuns);
        }

        public void execute() throws InterruptedException {




            state.board();
            state.departure();



        }
    }

    static class FanB extends TemplateThread {

        public FanB(int numRuns) {
            super(numRuns);
        }

        public void execute() throws InterruptedException {
            state.board();
            state.departure();
        }

    }


    static Euro2016State state = new Euro2016State();

    public static void main(String[] args) {
        for (int i = 0; i < 15; i++)
            run();
    }

    public static void run() {
        try {
            int numRuns = 1;
            int numIterations = 120;

            HashSet<Thread> threads = new HashSet<Thread>();

            for (int i = 0; i < numIterations; i++) {
                FanA h = new FanA(numRuns);
                FanB s = new FanB(numRuns);
                threads.add(h);
                threads.add(s);
            }

            init();

            ProblemExecution.start(threads, state);
            System.out.println(new Date().getTime());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}

class Euro2016State extends AbstractState {

    private static final int MAXIMUM_4_FANS_POINTS = 10;
    private static final int RACE_CONDITION_POINTS = 25;
    private static final int NOT_ALL_COMBINATIONS_INCLUDED_POINTS = 10;
    private static final int INVALID_COMBINATION_POINTS = 25;

    private static final String MAXIMUM_4_FANS_MESSAGE = "Vo taksito ima poveke od cetvorica.";
    private static final String MORE_THEN_ONE_CALL_OF_DEPARTURE = "Poveke od 1 patnik ja povikuva departure()";
    private static final String NOT_ALL_COMBINATIONS_INCLUDED_MESSAGE = "Ne gi dozvoluvate site kombinacii za kacuvanje.";
    private static final String CALL_OF_DEPARTURE = "Ja povikuvam departure()";

    private BoundCounterWithRaceConditionCheck Boat;

    private HashSet<String> combinations;

    private int fansA = 0;
    private int fansB = 0;
    private long checkRaceConditionInt;

    public Euro2016State() {
        combinations = new HashSet<String>();
        Boat = new BoundCounterWithRaceConditionCheck(0, 4,
                MAXIMUM_4_FANS_POINTS,
                MAXIMUM_4_FANS_MESSAGE, null, 0, null);
        checkRaceConditionInt = Long.MIN_VALUE;
    }

    public synchronized void board() {

        log(Boat.incrementWithMax(false), "se kacuvam vo taksi");
        Thread t = Thread.currentThread();
        if (t instanceof Euro2016.FanA) {
            fansA++;
        } else {
            fansB++;
        }
    }

    public void departure() {
        checkRaceConditionInt++;
        if (fansA + fansB == 4 && fansA % 2 == 0 && fansB % 2 == 0) {
            combinations.add(encode());

            RaceConditionMethod();

            log(null, CALL_OF_DEPARTURE);

            fansA = 0;
            fansB = 0;
            Boat.setValue(0);
        } else {
            logException(new PointsException(INVALID_COMBINATION_POINTS,
                    InvalidCombinationMessage()));
        }
    }

    private String InvalidCombinationMessage() {
        return "Nevalidna kombinacija na fansA so fansB. FansA number : "
                + fansA + " FansB number : " + fansB;
    }

    private void RaceConditionMethod() {
        long check;
        synchronized (this) {
            check = checkRaceConditionInt;
        }
        Switcher.forceSwitch(3);

        if (check != checkRaceConditionInt) {
            logException(new PointsException(RACE_CONDITION_POINTS,
                    MORE_THEN_ONE_CALL_OF_DEPARTURE));
        }
    }

    public String encode() {
        return fansA + " " + fansB;
    }

    @Override
    public void finalize() {

        if (combinations.size() != 3) {
            logException(new PointsException(
                    NOT_ALL_COMBINATIONS_INCLUDED_POINTS,
                    NOT_ALL_COMBINATIONS_INCLUDED_MESSAGE));
        }

    }
}