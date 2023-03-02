import java.io.File;
import java.util.List;
import java.util.Random;

import com.gams.api.GAMSWorkspace;
import com.gams.api.GAMSWorkspaceInfo;


public class Main {
	final static String inputFolder = "inputs";
	final static String outputFolder = "outputs";
	final static String testFolder = "tests";
	final static String fileNameTest = "tests2run.txt";
	final static String sufixFileOutput = "_outputs.txt";
	public static void main( String[] args ) {
		System.out.println("****  WELCOME TO THIS PROGRAM  ****");
		long programStart = ElapsedTime.systemTime();
		int counter = 0;
		/* 1. GET THE LIST OF TESTS TO RUN FORM "test2run.txt" */ //Parameter file not yet included, hard coded in problem!
		String testsFilePath = testFolder + File.separator + fileNameTest;
		List<Test> testsList = TestsReader.getTestsList(testsFilePath);

		/* 2. FOR EACH TEST (instanceName + testParameters) IN THE LIST... */
		for(Test currentTest: testsList)
		{   

			
			//******************************************************
			
			  GAMSWorkspaceInfo  wsInfo  = new GAMSWorkspaceInfo();
		         if (args.length > 0)
		             wsInfo.setSystemDirectory( args[0] );
		         // create a directory
		         File workingDirectory = new File(System.getProperty("user.dir"), "hhc");
		         workingDirectory.mkdir();
		         wsInfo.setWorkingDirectory(workingDirectory.getAbsolutePath());
		         // create a workspace
		         GAMSWorkspace ws = new GAMSWorkspace(wsInfo);
			
			
		     //******************************************************
			Random rng = new Random(currentTest.getSeed()); // Random number generator
			System.out.println("\nSTARTING TEST " + (++counter) + " OF " + testsList.size());

			// 2.1 GET THE INSTANCE INPUTS (DATA ON NODES AND VEHICLES)
			// "instanceName_input_nodes.txt" contains data on nodes
			String inputSource = inputFolder + File.separator +
					currentTest.getInstanceName() + File.separator;
			Inputs inputs = InputsReader.readInputs(inputSource,rng);
			long t = System.nanoTime();
			String objective="";
			if(currentTest.getdriverObjective()==1 && currentTest.gethomeCareStaffObjective()==0) {
				objective="Driver";
			}
			else {
				if(currentTest.getdriverObjective()==0 && currentTest.gethomeCareStaffObjective()==1) {
					objective="Home_Care_Staff";	
				}
				else { // integrated
					objective="Integrated";	
				}
			}
			String outputsFilePath = outputFolder + File.separator +
					currentTest.getInstanceName() + "_" + currentTest.getSeed() +"_"+objective+"_between"+currentTest.getWalking2jobs()+"_cum"+currentTest.getCumulativeWalkingTime()+  sufixFileOutput;
			// 2.2. USE THE MULTI-START ALGORITHM TO SOLVE THE INSTANCE
			//Algorithm algorithm = new Algorithm(currentTest, inputs, rng);
			Algorithm algorithm = new Algorithm(currentTest, inputs, rng,ws);
			Outputs output = new Outputs(currentTest,algorithm);
			Double endTime=(System.nanoTime() - t) / Math.pow(10, 6);
			output.sendToFile(outputsFilePath,endTime);

			// algorithm.solve(outputsFilePath);
			System.out.println("Taked:"+endTime);
		}

		/* 3. END OF PROGRAM */
		System.out.println("\n****  END OF PROGRAM, CHECK OUTPUTS FILES  ****");
		long programEnd = ElapsedTime.systemTime();
		System.out.println("Total elapsed time = "
				+ ElapsedTime.calcElapsedHMS(programStart, programEnd));
	}

}
