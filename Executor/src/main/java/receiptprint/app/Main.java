package receiptprint.app;

import receiptprint.system.DataDispatcher;


public class Main {
    public static void main(String[] args) {
        DataDispatcher dispatcher = new DataDispatcher();
        dispatcher.run();
    }
}
