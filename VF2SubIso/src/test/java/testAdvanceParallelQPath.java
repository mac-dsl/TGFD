package test.java;

import main.java.ParalleRunner.AdvancedCoordinatorWithReBalanceAndQPath;
import main.java.ParalleRunner.AdvancedWorkerWithRebalanceAndQPath;
import main.java.Util.Config;

import java.io.FileNotFoundException;

public class testAdvanceParallelQPath {

    public static void main(String []args) throws FileNotFoundException {
        Config.parse(args[0]);
        if(Config.nodeName.equalsIgnoreCase("coordinator"))
        {

            AdvancedCoordinatorWithReBalanceAndQPath coordinator =new AdvancedCoordinatorWithReBalanceAndQPath();
            coordinator.start();
            coordinator.assignJobs();
            coordinator.waitForResults();
        }
        else
        {
            System.out.println("Worker '"+ Config.nodeName+"' is starting...");
            AdvancedWorkerWithRebalanceAndQPath worker=new AdvancedWorkerWithRebalanceAndQPath();
            worker.start();
        }
    }
}
