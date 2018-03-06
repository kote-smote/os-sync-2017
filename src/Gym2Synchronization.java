import java.util.Date;
import java.util.HashSet;
import java.util.Scanner;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import sync.*;

public class Gym2Synchronization {
    static Semaphore gym;
    static Semaphore canSport;
    static Semaphore dressingRoom;
    static int playersNum;
    static Lock lock;

    public static void init() {
        gym = new Semaphore(12);
        canSport = new Semaphore(0);
        dressingRoom = new Semaphore(4);
        playersNum = 0;
        lock = new ReentrantLock();
    }


    public static class Player extends TemplateThread {

        public Player(int playersNumRuns) {
            super(playersNumRuns);
        }

        @Override
        public void execute() throws InterruptedException {
            gym.acquire();
            state.vlezi();

            lock.lock();
            if (++playersNum == 12) {
                lock.unlock();
                canSport.release(11);

            } else {
                lock.unlock();
                canSport.acquire();
            }
            
            state.sportuvaj();
            dressingRoom.acquire();
            state.presobleci();
            dressingRoom.release();
            synchronized (lock) {
                if (--playersNum == 0) {
                    state.slobodnaSala();
                    gym.release(12);
                }
            }
        }

    }
    static Gym2State state = new Gym2State();

    public static void main(String[] args) {
        for (int i = 0; i < 10; i++) {
            run();
        }
    }

    public static void run() {
        try {
            Scanner s = new Scanner(System.in);
            int playersNumRuns = 1;
            int playersNumIterations = 1200;
            s.close();

            HashSet<Thread> threads = new HashSet<Thread>();

            for (int i = 0; i < playersNumIterations; i++) {
                Player h = new Player(playersNumRuns);
                threads.add(h);
            }

            init();

            ProblemExecution.start(threads, state);
            System.out.println(new Date().getTime());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}


class Gym2State extends AbstractState {

    private static final int MAXIMUM_4_PLAYERS_POINTS = 8;
    private static final int MAXIMUM_12_PLAYERS_POINTS = 8;
    private static final int START_PLAY_POINTS = 10;
    private static final int FINISHED_PLAY_POINTS = 8;
    private static final int DRESSING_NOT_PARALLEL_POINTS = 8;
    private static final int PLAYING_NOT_PARALLEL_POINTS = 8;

    private static final String MAXIMUM_4_PLAYERS = "Poveke od 4 igraci se presoblekuvaat istovremeno!!!";
    private static final String MAXIMUM_12_PLAYERS = "Poveke od 12 igraci igraat istovremeno!!!";
    private static final String START_PLAY_MESSAGE = "Ne se prisutni 12 igraci za da zapocne igranjeto!!!";
    private static final String FINISHED_PLAY_MESSAGE = "Ne moze da se zatvori saalta. Seuste ima igraci vo nea!!!";
    private static final String DRESSING_NOT_PARALLEL = "Presoblekuvanjeto ne e paralelizirano!!!";
    private static final String PLAYING_NOT_PARALLEL = "Ne moze da se igra sam po sam!!!";

    private BoundCounterWithRaceConditionCheck dressingRoom;
    private BoundCounterWithRaceConditionCheck play;
    private BoundCounterWithRaceConditionCheck gymedPlayers;
    private BoundCounterWithRaceConditionCheck finishedPlayers;

    public Gym2State() {
        dressingRoom = new BoundCounterWithRaceConditionCheck(0, 4,
                MAXIMUM_4_PLAYERS_POINTS, MAXIMUM_4_PLAYERS, null, 0, null);
        play = new BoundCounterWithRaceConditionCheck(0, 12,
                MAXIMUM_12_PLAYERS_POINTS, MAXIMUM_12_PLAYERS, null, 0, null);

        gymedPlayers = new BoundCounterWithRaceConditionCheck(0);
        finishedPlayers = new BoundCounterWithRaceConditionCheck(0);

    }

    public void vlezi() {
        gymedPlayers.incrementWithMax(false);
    }

    /**
     * Treba da se presobleceni 12 igraci za da zapocne igrata
     */
    public void sportuvaj() {
        log(gymedPlayers.assertEquals(12, START_PLAY_POINTS,
                START_PLAY_MESSAGE), "zapocnuvam na sportuvam");
        log(play.incrementWithMax(false), null);
        Switcher.forceSwitch(10);
        log(play.decrementWithMin(false), null);
    }

    /**
     * Moze da se presoblekuvaat maksimum 4 paralelno. Ne treba eden po eden.
     */
    public void presobleci() {
        log(dressingRoom.incrementWithMax(false), "se presoblekuvam");
        Switcher.forceSwitch(10);
        log(dressingRoom.decrementWithMin(false), null);
        log(finishedPlayers.incrementWithMax(false), null);
    }

    /**
     * Treba site 12 igraci da zavrsile so igranjeto. Se povikuva samo od eden.
     */
    public void slobodnaSala() {
        log(finishedPlayers.assertEquals(12, FINISHED_PLAY_POINTS,
                FINISHED_PLAY_MESSAGE), "zatvoram sala");
        log(gymedPlayers.checkRaceCondition(), null);
        gymedPlayers.setValue(0);
        finishedPlayers.setValue(0);
    }

    @Override
    public void finalize() {
        if (dressingRoom.getMax() == 1) {
            logException(new PointsException(DRESSING_NOT_PARALLEL_POINTS,
                    DRESSING_NOT_PARALLEL));
        }

        if (play.getMax() == 1) {
            logException(new PointsException(PLAYING_NOT_PARALLEL_POINTS,
                    PLAYING_NOT_PARALLEL));
        }
    }

}
