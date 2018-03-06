import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Semaphore;

public class ProducerController {

    //TODO: Definicija na globalni promenlivi i semafori
    // Vo ova resenie kontrolata e kaj controllerot
    Semaphore bufferAccess; // globalen semafor za pristap do baferot
    int numChecks;
    Semaphore lock; // zat pristap do numChecks bidejki taa promenliva e vo kriticna oblas
    Semaphore canCheck;

    public void init() {
        //TODO: da se implementira
        bufferAccess = new Semaphore(1);
        numChecks = 0;
        lock = new Semaphore(1);
        canCheck = new Semaphore(10);
    }

    class Proizveduvac extends Thread {

        public void dodadi() throws InterruptedException {
            //TODO: da se implementira
            bufferAccess.acquire();
            buffer.dodadi();
            bufferAccess.release();
        }

        @Override
        public void run() {
            try {
                for (int i = 0; i < BROJ_AKCII&&!hasException; i++) {
                    dodadi();
                    iteration++;
                }
            } catch (InterruptedException e) {
                // Do nothing
            } catch (Exception e) {
                exception = e;
                hasException = true;
            }
        }

        @Override
        public String toString() {
            return String.format("p\t%d\t%d", getId(), iteration);
        }
        public Exception exception = null;
        public int iteration = 0;
    }

    class Kontrolor extends Thread {

        public void proveri() throws InterruptedException {
            //TODO: da se implementira
            lock.acquire();
            if (numChecks == 0) // ako sum prv
                bufferAccess.acquire();
            numChecks++;
            lock.release();

            canCheck.acquire();
            buffer.proveri();
            lock.acquire();
            numChecks--;
            canCheck.release();
            if (numChecks == 0) // ako sum posleden
                bufferAccess.release();
            lock.release();
        }

        @Override
        public void run() {
            try {
                for (int i = 0; i < BROJ_AKCII && !hasException; i++) {
                    proveri();
                    iteration++;
                }
            } catch (InterruptedException e) {
                // Do nothing
            } catch (Exception e) {
                exception = e;
                hasException = true;
            }
        }

        @Override
        public String toString() {
            return String.format("k\t%d\t%d", getId(), iteration);
        }
        public Exception exception = null;
        private int iteration = 0;
    }

    public static void main(String[] args) {
        try {
            ProducerController environment = new ProducerController();
            environment.start();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void start() throws Exception {
        buffer = new Buffer();
        init();
        HashSet<Thread> threads = new HashSet<Thread>();
        for (int i = 0; i < BROJ_INSTANCI; i++) {
            Kontrolor z = new Kontrolor();
            Proizveduvac m = new Proizveduvac();
            threads.add(z);
            threads.add(m);
        }

        for (Thread t : threads) {
            t.start();
        }

        boolean valid = true;
        for (Thread t : threads) {
            if (!hasException) {
                t.join();
            } else {
                t.interrupt();
            }
            if (t instanceof Proizveduvac) {
                Proizveduvac d = (Proizveduvac) t;
                if (d.exception != null) {
                    valid = false;
                }
            }
            if (t instanceof Kontrolor) {
                Kontrolor r = (Kontrolor) t;
                if (r.exception != null) {
                    valid = false;
                }
            }
        }
        if (valid) {
            System.out.println("Procesot e uspesno sinhroniziran");
        } else {
            System.out.println("Procesot ne e sinhroniziran spored uslovite na zadacata");
            buffer.printLog();
        }


    }

    public class Buffer {

        public Buffer() {
        }
        private int brojProverki = 0;
        private boolean imaDodavanje = false;

        public void dodadi() throws RuntimeException {
            synchronized (RANDOM) {
                log(null, "dodavanje start");
                if (brojProverki > 0) {
                    RuntimeException e = new RuntimeException("Ne moze se dodade element vo baferot. Vo momentot ima aktivni proverki.");
                    log(e, null);
                    throw e;
                }
                if (imaDodavanje) {
                    RuntimeException e = new RuntimeException("Ne moze se dodade element vo baferot. Vo momentot ima drugo dodavanje.");
                    log(e, null);
                    throw e;
                }
                imaDodavanje = true;
            }
            try {
                int r;
                synchronized (RANDOM) {
                    r = RANDOM.nextInt(RANDOM_RANGE);
                }
                Thread.sleep(r);
            } catch (Exception e) {
                //do nothing
            }
            synchronized (RANDOM) {
                imaDodavanje = false;
                log(null, "dodavanje kraj");
            }
        }

        public void proveri() throws RuntimeException {
            synchronized (RANDOM) {
                log(null, "proveri start");
                if (brojProverki > 10) {
                    RuntimeException e = new RuntimeException("Ne moze se proveri element od baferot. Vo momentot ima 10 aktivni proverki.");
                    log(e, null);
                    throw e;
                }
                if (imaDodavanje) {
                    RuntimeException e = new RuntimeException("Ne moze se proveri element od baferot. Vo momentot ima drugo dodavanje.");
                    log(e, null);
                    throw e;
                }

                brojProverki++;
            }
            try {
                int r;
                synchronized (RANDOM) {
                    r = RANDOM.nextInt(RANDOM_RANGE);
                }
                Thread.sleep(r);
            } catch (Exception e) {
                //do nothing
            }
            synchronized (RANDOM) {
                brojProverki--;
                log(null, "proveri end");
            }
        }
        private List<String> actions = new ArrayList<String>();

        private synchronized void log(RuntimeException e, String action) {
            Thread t = Thread.currentThread();
            if (e == null) {
                actions.add(t.toString() + "\t(a): " + action);
            } else {
                actions.add(t.toString() + "\t(e): " + e.getMessage());
            }
        }

        public synchronized void printLog() {
            System.out.println("Poradi konkurentnosta za pristap za pecatenje, mozno e nekoja od porakite da ne e na soodvetnoto mesto.");
            System.out.println("Log na izvrsuvanje na akciite:");
            System.out.println("=========================");
            System.out.println("(tip p<=>Proizveduvac, tip k<=>Kontrolor)");
            System.out.println("tip\tid\titer\takcija/error");
            System.out.println("=========================");
            for (String l : actions) {
                System.out.println(l);
            }
        }
    }
    // Konstanti
    public static int BROJ_INSTANCI = 50;
    public static int BROJ_AKCII = 50;
    public static final Random RANDOM = new Random();
    public static final int RANDOM_RANGE = 2;
    // Instanca od bafferot
    public Buffer buffer;
    public boolean hasException = false;
}
