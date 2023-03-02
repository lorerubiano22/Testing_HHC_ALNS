import java.io.*;
import java.lang.reflect.Array;

import com.gams.api.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;



/**
 * @author Lorena
 *
 */
public class Algorithm {


	private HashMap<Integer,Jobs> jobsList = new HashMap<Integer, Jobs>();
	private LinkedList<Jobs> patientList= new LinkedList<Jobs>(); // pick up first request for each patient
	private LinkedList<Jobs> dropOfpatientsList= new LinkedList<Jobs>();
	private LinkedList<Jobs> dropOfMedicalCenterList= new LinkedList<Jobs>();
	private LinkedList<Jobs> pickUPMedicalCenterList= new LinkedList<Jobs>();
	private LinkedList<Jobs> clientList= new LinkedList<Jobs>(); // drop off first request for  each client
	private LinkedList<Jobs> pickUPNursesList= new LinkedList<Jobs>();
	private LinkedList<Jobs> allRequestsList= new LinkedList<Jobs>();
	boolean[][] relationship;
	private HashMap<Integer, MedicalStaff> completeStaff = new HashMap<Integer, MedicalStaff>();
	private ArrayList<MedicalStaff> copyfrom_completeStaff=new ArrayList<MedicalStaff>();
	private ArrayList<MedicalStaff> nursesQ1;
	private ArrayList<MedicalStaff> nursesQ2;
	private ArrayList<MedicalStaff> nursesQ3;
	private ArrayList<MedicalStaff> medicalStaff;
	int[] assigmentMedicalStaff;
	private ArrayList<Vehicle> vehicleList;
	private Solution initialSolution;


	public Algorithm(Test currentTest, Inputs inputs, Random rng, GAMSWorkspace ws) {
		initialSolution= new Solution();
		dataPreparation(currentTest,inputs,rng);
		initialSolution(currentTest,inputs,rng,ws);

	}



	private void initialSolution(Test currentTest, Inputs inputs, Random rng, GAMSWorkspace ws) {
		dummyAssigmentPersonalToPatients(inputs);
		improveAssigmentPersonalToPatients(currentTest,inputs);
		routing(currentTest,inputs,rng);
		GAMSSOLVER.definingArrivalsTime(vehicleList, ws,currentTest,inputs,rng,relationship);
		initialSolution.setSchedulingandRouting(vehicleList);
	
		System.out.print(initialSolution.toString());
	}




	private void routing(Test currentTest, Inputs inputs, Random rng) {

		// under the assumption that one vehicle can have more than one route
		vehicleList= new ArrayList<Vehicle> ();
		for(int i=0;i<inputs.getVehicles().get(0).getQuantity();i++ ) {
			Vehicle v= new Vehicle(i);
			vehicleList.add(v);
		}

		ArrayList<Jobs> assigned= new ArrayList<Jobs> ();
		for(int i=0;i<vehicleList.size();i++) {// if insterted node is + then should find the negative
			for(MedicalStaff s:copyfrom_completeStaff) { // all vehicle are
				Vehicle v=vehicleList.get(i);
				Jobs jobsToInsert=s.getAvailability().get(0);
				if(v.getJobs().isEmpty()) {
					insertingFirstJobs(jobsToInsert,v,s,assigned,inputs);
				}
			}
			System.out.print(vehicleList.get(i).toString());
		}
		insertingOtherJobs_hardTW(vehicleList,assigned,inputs,currentTest); // all drop-off at medical center and drop off nurse
		insertingOtherJobs_softTW(vehicleList,assigned,inputs,currentTest);

		
		
		for(MedicalStaff s:copyfrom_completeStaff) {  // validation
			for(Jobs j:s.getAvailability()) { // jobs
				if(!assigned.contains(j)) {
					System.out.print("stop");	
				}
			}
		}
		System.out.print("Done");
	}



	private void insertingOtherJobs_softTW(ArrayList<Vehicle> vehicleList, ArrayList<Jobs> assigned, Inputs inputs,
			Test currentTest) {
		for(MedicalStaff s:copyfrom_completeStaff) { // all vehicle are
			HashMap<Integer,ArrayList<Double>> vehJob= new  HashMap<Integer,ArrayList<Double>> ();// First compute the best position for each vehicle
			for(int j=0;j<s.getAvailability().size();j++) {
				double[] insertionCostperVehicle= new double[vehicleList.size()];
				if(!assigned.contains(s.getAvailability().get(j))  && !s.getAvailability().get(j).isPatient()) {
					for(Vehicle v: vehicleList) {
						ArrayList<Double> cost_position= new ArrayList<Double>(); // first vehicle, second position
						double[] insertionCost=insertJob(s.getAvailability().get(j),v,s,inputs);
						cost_position=selectCheapestVehPosition(insertionCost); // retorn the position
						double insertionCost_in_position=storeCostInsertion(insertionCost); // retorn the cost
						insertionCostperVehicle[v.getVehicleID()]=insertionCost_in_position;
						cost_position.add(0, (double)v.getVehicleID());
						cost_position.add(insertionCost_in_position);
						vehJob.put(v.getVehicleID(), cost_position); // for each job the insertion cost is stored for each job
					}
					insertingJob(insertionCostperVehicle, vehJob,s.getAvailability().get(j),s,vehicleList, assigned, inputs,currentTest);
				}
			}
		}
	}



	private boolean softTimeWindow(Jobs jobs) {
		boolean softTW= false;
		if((jobs.isClient() && jobs.getTotalPeople()>0) || (jobs.isMedicalCentre() && jobs.getTotalPeople()>0)) {
			softTW= true;
		}
		return softTW;
	}



	private void insertingOtherJobs_hardTW(ArrayList<Vehicle> vehicleList, ArrayList<Jobs> assigned, Inputs inputs, Test currentTest) {
		for(MedicalStaff s:copyfrom_completeStaff) { // all vehicle are
			HashMap<Integer,ArrayList<Double>> vehJob= new  HashMap<Integer,ArrayList<Double>> ();// First compute the best position for each vehicle
			for(int j=0;j<s.getAvailability().size();j++) {
				double[] insertionCostperVehicle= new double[vehicleList.size()];
				if(!assigned.contains(s.getAvailability().get(j)) & hardTimeWindow(s.getAvailability().get(j))) {
					for(Vehicle v: vehicleList) {
						ArrayList<Double> cost_position= new ArrayList<Double>(); // first vehicle, second position
						double[] insertionCost=insertJob(s.getAvailability().get(j),v,s,inputs);
						cost_position=selectCheapestVehPosition(insertionCost); // retorn the position
						double insertionCost_in_position=storeCostInsertion(insertionCost); // retorn the cost
						insertionCostperVehicle[v.getVehicleID()]=insertionCost_in_position;
						cost_position.add(0, (double)v.getVehicleID());
						cost_position.add(insertionCost_in_position);
						vehJob.put(v.getVehicleID(), cost_position); // for each job the insertion cost is stored for each job
					}
					insertingJob(insertionCostperVehicle, vehJob,s.getAvailability().get(j),s,vehicleList, assigned, inputs,currentTest);
				}
			}
		}
	}







	private double storeCostInsertion(double[] insertionCost) {
		double minCost=Double.MAX_VALUE;
		for(int i=0;i<insertionCost.length;i++) {
			if(insertionCost[i]<minCost) {
				minCost=insertionCost[i];
			}
		}


		return minCost;
	}




	private void insertingJob(double[] insertionCostperVehicle, HashMap<Integer, ArrayList<Double>> vehJob, Jobs jobs, MedicalStaff s,
			ArrayList<Vehicle> vehicleList, ArrayList<Jobs> assigned, Inputs inputs, Test currentTest) {
		int vehID=cheapestVeh(insertionCostperVehicle);// return veh ID

		double bestPosition=-1;
		ArrayList<Double> veh=vehJob.get(vehID);	// LIST OF VEHICLE FOT THE JOB

		bestPosition=veh.get(1);	// best position
		insertionJob(jobs,vehicleList.get(vehID),s,bestPosition,assigned,inputs);		// insertJob with the complement node

		System.out.print(vehicleList.get(vehID).toString());		

	}






	private int cheapestVeh(double[] insertionCostperVehicle) {
		double minCost=Double.MAX_VALUE;
		int veh=-1;
		for(int i=0;i<insertionCostperVehicle.length;i++) {
			if(insertionCostperVehicle[i]<minCost) {
				minCost=insertionCostperVehicle[i];
				veh=i;
			}
		}
		return veh;
	}



	private void insertionJob(Jobs jobs, Vehicle vehicle, MedicalStaff s, double bestPosition, ArrayList<Jobs> assigned, Inputs inputs) {
		assigned.add(jobs);
		vehicle.getJobs().add((int)bestPosition,jobs);
		vehicle.setCurrentLoad(jobs);
		if(jobs.isMedicalCentre()) { // add the complement
			if(jobs.getTotalPeople()<0) {
				for(Jobs jj:s.getAvailability()) {
					if(relationship[jj.getIDrequest()][jobs.getIDrequest()]==true) {
						assigned.add(jj);
						vehicle.getJobs().add((int)bestPosition,jj);
						vehicle.setCurrentLoad(jj);
						break;
					}
				}
			}
			else {
				for(Jobs jj:s.getAvailability()) {
					if(relationship[jobs.getIDrequest()][jj.getIDrequest()]==true) {
						assigned.add(jj);
						vehicle.getJobs().add((int)bestPosition+1,jj);
						vehicle.setCurrentLoad(jj);
						break;
					}
				}
			}
		}
		if(jobs.isClient()) {
			if(jobs.getTotalPeople()<0) {
				for(Jobs jj:s.getAvailability()) {
					if(relationship[jobs.getIDrequest()][jj.getIDrequest()]==true) {
						assigned.add(jj);
						vehicle.getJobs().add((int)bestPosition+1,jj);
						vehicle.setCurrentLoad(jj);
						break;
					}
				}
			}		
		}
	}


	//cheapestVehicle= selectionBestVehicle(insertionCostperVehicle);
	private int selectionBestVehicle(double[] insertionCostperVehicle) {
		int cheapestVeh=0;
		double minCost=Double.MAX_VALUE;
		double bestPosition=-1;

		for(int i=0;i<insertionCostperVehicle.length;i++) {
			if(insertionCostperVehicle[i]<minCost) {
				cheapestVeh=i;
			}
		}
		return cheapestVeh;
	}



	private ArrayList<Double> selectCheapestVehPosition(double[] insertionCost) {
		ArrayList<Double> inf= new ArrayList<Double>(); // first vehicle, second position
		double minCost=Double.MAX_VALUE;
		double bestPosition=-1;

		for(int i=0;i<insertionCost.length;i++) {
			if(insertionCost[i]<minCost) {
				minCost=insertionCost[i];
				bestPosition=i;
			}
		}
		inf.add(bestPosition);

		return inf;
	}



	private boolean hardTimeWindow(Jobs jobs) {
		boolean hardTW= false;
		if((jobs.isClient() && jobs.getTotalPeople()<0) || (jobs.isMedicalCentre() && jobs.getTotalPeople()<0)) {
			hardTW= true;
		}
		return hardTW;
	}



	private double[] insertJob(Jobs j, Vehicle v, MedicalStaff s, Inputs inputs) {
		double[] insertionCost= new double[v.getJobs().size()+1];

		if(v.hasCapacity()) {
			int i=0;// i indicates position

			for(i=0;i<=v.getJobs().size();i++) {
				insertionCost[i]=computeInsertionCost(i,j,v,inputs);
			}

		}
		else {
			for(int i=0;i<=v.getJobs().size()+1;i++) {
				insertionCost[i]=Double.MAX_VALUE;
			}
		}
		return insertionCost;
	}





	private double computeInsertionCost(int i, Jobs j, Vehicle v, Inputs inputs) {
		double startTime[]=new double[v.getJobs().size()+1];
		double endTime[]=new double[v.getJobs().size()+1];
		double arrivalTime[]=new double[v.getJobs().size()+1];
		double waitingTime[]=new double[v.getJobs().size()+1];
		double delay[]=new double[v.getJobs().size()+1];
		int lastJob=0;
		int position=0;
		double tv=0;
		double insertionCost=-Double.MIN_VALUE;
		ArrayList<Jobs>assigned= new ArrayList<Jobs>();
		//for(int position=0;position<v.getJobs().size();position++) {


		for(Jobs jj:v.getJobs()) {
			if(!assigned.contains(jj)) {
				if(position==i) {

					if(position==0) {
						arrivalTime[position]=j.getStartTime();
						startTime[position]=j.getStartTime();
						endTime[position]=j.getEndTime();
					}
					else {
						tv=inputs.getCarCost().getCost(lastJob-1, j.getId()-1);
						arrivalTime[position+1]=arrivalTime[position]+tv;
						startTime[position+1]=j.getStartTime();
						endTime[position+1]=j.getEndTime();
					}
					position++;
					lastJob=j.getId();
					tv=inputs.getCarCost().getCost(lastJob-1, jj.getId()-1);
					arrivalTime[position]=arrivalTime[position-1]+tv;
					startTime[position]=jj.getStartTime();
					endTime[position]=jj.getEndTime();
					lastJob=jj.getId();
					position++;
				}
				else {
					if(position==0) {
						arrivalTime[position]=jj.getStartTime();
						startTime[position]=jj.getStartTime();
						endTime[position]=jj.getEndTime();

					}
					else {
						tv=inputs.getCarCost().getCost(lastJob-1, jj.getId()-1);
						arrivalTime[position]=arrivalTime[position-1]+tv;
						startTime[position]=jj.getStartTime();
						endTime[position]=jj.getEndTime();
					}
					position++;
					lastJob=jj.getId();
					assigned.add(jj);
				}
			}

		}
		if(position==i && i>0) {
			tv=inputs.getCarCost().getCost(lastJob-1, j.getId()-1);
			arrivalTime[position]=arrivalTime[position-1]+tv;
			startTime[position]=j.getStartTime();
			endTime[position]=j.getEndTime();
		}


		for(position=0;position<=v.getJobs().size();position++) {
			waitingTime[position]=Math.max(startTime[position]-arrivalTime[position], 0);
			if(insertionCost<waitingTime[position]) {
				insertionCost=waitingTime[position];
			}
			delay[position]=Math.max(endTime[position]-arrivalTime[position], 0);
			if(insertionCost<delay[position]) {
				insertionCost=delay[position];
			}
		}
		return insertionCost;
	}






	private void insertingFirstJobs(Jobs j, Vehicle v, MedicalStaff s, ArrayList<Jobs> assigned, Inputs inputs) {
		if(!assigned.contains(j)) {
			v.getJobs().add(j);
			v.setCurrentLoad(j);
			j.setarrivalTime(j.getStartTime());

			assigned.add(j);
			//if(j.getTotalPeople()>0) {
			for(Jobs jj:s.getAvailability()) {
				if(relationship[j.getIDrequest()][jj.getIDrequest()]==true) {
					v.getJobs().add(jj);
					v.setCurrentLoad(jj);
					assigned.add(jj);
					break;
				}
			}
			//}
		}
	}



	private void dataPreparation(Test currentTest, Inputs inputs, Random rng) {
		// Patient List   total persons
		// +1 refers to pick-up and -1 drop-off
		for(Jobs j:inputs.getNodes()) {

			if(j.getReqQualification()>0) { // clients
				Jobs client=new Jobs(j); // pick up nurse
				client.setserviceTime(2);
				j.setClient(true);
				client.setClient(true);
				settingTWclients(j,client);
				clientList.add(j);

				j.setTotalPeople(-1,0);
				pickUPNursesList.add(client);
				client.setTotalPeople(1,0);

				allRequestsList.add(j);// adding as a list of request
				allRequestsList.add(client); // adding as a list of request
			}

			if(j.getReqQualification()==0 && j.getId()!=0 && !inputs.getMedicalCentre().containsKey(j.getId())) { // patients
				j.setTotalPeople(1,0);// pick up patient
				patientList.add(j);
				j.setPatient(true);
				Jobs pair=new Jobs(j.getsubJobPair()); // drop off at medical centre
				pair.setTotalPeople(-1,j.getId());
				pair.setMedicalCentre(true);

				Jobs pickUp=new Jobs(pair);  // pick up at medical centre
				pickUp.setMedicalCentre(true);
				Jobs patient=new Jobs(j); // drop ff up patient
				patient.setPatient(true);
				patient.setTotalPeople(-1,0);
				pickUp.setTotalPeople(1,patient.getId());

				settingTWhomeMedicalC(j,pair,pickUp,patient);
				dropOfpatientsList.add(patient);
				pickUPMedicalCenterList.add(pickUp);
				dropOfMedicalCenterList.add(pair);

				// adding as a list of request
				allRequestsList.add(j);
				allRequestsList.add(pair);
				allRequestsList.add(pickUp);
				allRequestsList.add(patient);
			}


		}

		for(int i=0;i<allRequestsList.size();i++) {
			allRequestsList.get(i).setIDrequest(i);
			jobsList.put(i, allRequestsList.get(i));
		}

		assigmentMedicalStaff= new int[allRequestsList.size()];
		relationship= new boolean[allRequestsList.size()][allRequestsList.size()];

	}



	private void improveAssigmentPersonalToPatients(Test currentTest, Inputs inputs) {
		HashMap<Integer,ArrayList<MedicalStaff>> directoryMedicalStaff= new HashMap<Integer,ArrayList<MedicalStaff>>();


		definigSetToMergeWorks(currentTest,medicalStaff,directoryMedicalStaff);
		definigSetToMergeWorks(currentTest,nursesQ1,directoryMedicalStaff);
		definigSetToMergeWorks(currentTest,nursesQ2,directoryMedicalStaff);
		definigSetToMergeWorks(currentTest,nursesQ3,directoryMedicalStaff);

		for (Integer key : directoryMedicalStaff.keySet()) { // select each medical staff and try to merge with other medical
			if(!completeStaff.get(key).getAvailability().isEmpty() && !directoryMedicalStaff.get(key).isEmpty()) {
				ArrayList<MedicalStaff> value = directoryMedicalStaff.get(key);
				mergingSchedules(completeStaff.get(key),value,currentTest);
			}
			else {
				completeStaff.remove(key);
			}
		}
		System.out.println("********");
		System.out.println("New Schedule");
		System.out.println("********");
		System.out.println("");

		Iterator<Map.Entry<Integer, MedicalStaff>> it = completeStaff.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Integer, MedicalStaff> entry = it.next();
			MedicalStaff staff = entry.getValue();
			if (staff == null || staff.getAvailability().isEmpty()) {
				it.remove(); // remove the entry from the map
			}
		}

		for (MedicalStaff ms : completeStaff.values()) {
			copyfrom_completeStaff.add(ms);
			for(Jobs j:ms.getAvailability()) {
				assigmentMedicalStaff[j.getIDrequest()]=ms.getId();
				j.setPersonnel(ms.getId());
			}
			System.out.println(ms.toString());
		}

		copyfrom_completeStaff.sort(MedicalStaff.startTimefirstWork);

		System.out.println("Done");
	}



	private void mergingSchedules(MedicalStaff medicalStaff,ArrayList<MedicalStaff> value, Test currentTest) {

		ArrayList<Integer> evaluated= new ArrayList<Integer>();
		ArrayList<MedicalStaff> newValue = new ArrayList<MedicalStaff> ();

		for(MedicalStaff ss: value) {
			newValue.add(ss);
		}

		for(MedicalStaff ss: newValue) {
			if(!ss.getAvailability().isEmpty() && !evaluated.contains(ss.getId())) {
				Jobs lastJobs_s=medicalStaff.getAvailability().get(medicalStaff.getAvailability().size()-1);
				Jobs lastJobs_x=ss.getAvailability().get(0);
				if(medicalStaff.getQualification()== ss.getQualification() & lastJobs_s.getStartTime()<lastJobs_x.getEndTime() & medicalStaff.getWorking_hours()+ss.getWorking_hours()<=0.7*currentTest.getWorkingTime()) {
					evaluated.add(ss.getId());
					for(Jobs j: ss.getAvailability()) {
						medicalStaff.getAvailability().add(j);
					}
					ss.getAvailability().clear();
					ss.computeWorkingHours();
				}
			}
			medicalStaff.getAvailability().sort(Jobs.SORT_BY_STARTW);
			medicalStaff.computeWorkingHours();
			if(ss.getAvailability().isEmpty()) {
				value.remove(ss);
			}
		}

	}



	private void definigSetToMergeWorks(Test currentTest, ArrayList<MedicalStaff> nursesQ, HashMap<Integer, ArrayList<MedicalStaff>> directoryMedicalStaff) {

		for(MedicalStaff s: nursesQ) {
			if(!s.getAvailability().isEmpty()) { 
				ArrayList<MedicalStaff> ms= new ArrayList<MedicalStaff>();
				directoryMedicalStaff.put(s.getId(), ms);
				for(MedicalStaff x: nursesQ) {
					if(!s.equals(x) && !x.getAvailability().isEmpty()) { // qualification, working time and time windows
						Jobs lastJobs_s=s.getAvailability().get(s.getAvailability().size()-1);
						Jobs lastJobs_x=x.getAvailability().get(0);
						if(lastJobs_s.getStartTime()<lastJobs_x.getEndTime() & s.getWorking_hours()+x.getWorking_hours()<=0.7*currentTest.getWorkingTime()) {
							ms.add(x);
						}
					}
				}
				if(ms.size()>1) {
					ms.sort(MedicalStaff.startTimeLastWork);
				}
			}
		}
	}



	private void insertJobs(boolean[][] relationship, LinkedList<Jobs> allRequestsList2, ArrayList<MedicalStaff> nursesQ3, int i) {
		nursesQ3.sort(MedicalStaff.jobListSize);

		nursesQ3.get(0).getAvailability().add(allRequestsList.get(i));
		if(relationship[i][i+1]==true) {
			nursesQ3.get(0).getAvailability().add(allRequestsList.get(i+1));
		}
		if(i+2<allRequestsList.size()) {
			if(relationship[i][i+2]==true) {
				nursesQ3.get(0).getAvailability().add(allRequestsList.get(i+2));
			}
		}
		if(i+3<allRequestsList.size()) {
			if(relationship[i][i+3]==true) {
				nursesQ3.get(0).getAvailability().add(allRequestsList.get(i+3));
			}}
		if(i+4<allRequestsList.size()) {
			if(relationship[i][i+4]==true) {
				nursesQ3.get(0).getAvailability().add(allRequestsList.get(i+4));
			}}
	}



	private void settingTWclients(Jobs dropOffNurse, Jobs pickUpNurse) {

		double MaxTT=40;
		double MaxW=40;
		double MaxD=40;
		double loading=2;
		pickUpNurse.setStartTime(dropOffNurse.getStartTime()+dropOffNurse.getReqTime()+loading);
		pickUpNurse.setEndTime(pickUpNurse.getStartTime()+MaxW+loading);
	}



	private void settingTWhomeMedicalC(Jobs pickUphomePatient, Jobs dropOffatMedicalCentre, Jobs pickUpMedicalCenter, Jobs dropOffpatient ) {
		double originalStartTime=pickUphomePatient.getStartTime();
		double originalEndTime=pickUphomePatient.getEndTime();
		double durationService=pickUphomePatient.getReqTime();
		double MaxTT=40;
		double MaxW=40;
		double MaxD=40;
		double loading=2;
		pickUphomePatient.setStartTime(originalStartTime-MaxTT);
		pickUphomePatient.setEndTime(pickUphomePatient.getStartTime()+MaxTT); // assuming that people can departure at least with x min in advance to the apointment
		pickUphomePatient.setserviceTime(loading);

		dropOffatMedicalCentre.setStartTime(originalStartTime);
		dropOffatMedicalCentre.setEndTime(originalEndTime);
		dropOffatMedicalCentre.setserviceTime(durationService);

		pickUpMedicalCenter.setStartTime(originalStartTime+durationService); // assumming the optimistic scenarios in which the patient receive the medical treatment
		pickUpMedicalCenter.setEndTime(pickUpMedicalCenter.getStartTime()+MaxW); // assuming that people can departure at least with x min in advance to the apointment
		pickUpMedicalCenter.setserviceTime(loading);

		dropOffpatient.setStartTime(pickUpMedicalCenter.getStartTime()+loading); // asumming the optimistic scenarios in which the patient receive the medical treatment
		dropOffpatient.setEndTime(pickUpMedicalCenter.getEndTime()+MaxD); // worst scenario where the patient should wait and has a larger travel time
		dropOffpatient.setserviceTime(loading);

	}



	private void dummyAssigmentPersonalToPatients(Inputs inputs) {
		for(int i=0; i<allRequestsList.size();i++ ) { // building relation Matrix
			//for(int j=i+1; j<allRequestsList.size();i++ ) { // building relation Matrix
			if(i<(inputs.getclients().size()*2)) { // last client
				relationship[i][i+1]=true;
				i++;
			}
			if(i>=(inputs.getclients().size()*2)) { // last client
				relationship[i][i+1]=true;
				i++;
				relationship[i][i+1]=true;
				i++;
				relationship[i][i+1]=true;
				i++;
			}
			//}
		}


		// Medical staff --- 10 medical staff
		medicalStaff= new ArrayList<MedicalStaff> (10);

		for(int i=0;i<10;i++) {
			ArrayList<Jobs> ms= new ArrayList<Jobs>();
			MedicalStaff listMedicalstaff= new MedicalStaff(i, ms,0);
			medicalStaff.add(listMedicalstaff);


		}

		//allRequestsList.sort(Jobs.SORT_BY_STARTW);

		int paramedic=0;
		for(int i=0; i<allRequestsList.size();i++ ) { // initialize schedulling	& falta incluir las working hours
			if((allRequestsList.get(i).isPatient() && allRequestsList.get(i).getTotalPeople()>0) ) {// hard constraint
				medicalStaff.get(paramedic).getAvailability().add(allRequestsList.get(i));
				if(relationship[i][i+1]==true) {
					medicalStaff.get(paramedic).getAvailability().add(allRequestsList.get(i+1));

					if(i+2<allRequestsList.size()) {
						if(relationship[i+1][i+2]==true) {
							medicalStaff.get(paramedic).getAvailability().add(allRequestsList.get(i+2));
						}
					}

					if(i+3<allRequestsList.size()) {
						if(relationship[i+2][i+3]==true) {
							medicalStaff.get(paramedic).getAvailability().add(allRequestsList.get(i+3));
						}}
					if(i+4<allRequestsList.size()) {
						if(relationship[i+3][i+4]==true) {
							medicalStaff.get(paramedic).getAvailability().add(allRequestsList.get(i+4));
						}}
				}


				paramedic++;
			}
			if(paramedic==medicalStaff.size()-1) {
				paramedic=0;
			}
		}


		for(MedicalStaff ms:medicalStaff) {// adding id to the medical staff
			completeStaff.put(ms.getId(), ms);
		}

		nursesQ1= new ArrayList<MedicalStaff> (10);

		for(int i=10;i<20;i++) {
			ArrayList<Jobs> ms= new ArrayList<Jobs>();
			MedicalStaff listMedicalstaff= new MedicalStaff(i, ms,1);
			medicalStaff.add(listMedicalstaff);
			nursesQ1.add(listMedicalstaff);

		}

		nursesQ2= new ArrayList<MedicalStaff> (10);

		for(int i=20;i<30;i++) {
			ArrayList<Jobs> ms= new ArrayList<Jobs>();
			MedicalStaff listMedicalstaff= new MedicalStaff(i, ms,2);
			medicalStaff.add(listMedicalstaff);
			nursesQ2.add(listMedicalstaff);
		}

		nursesQ3= new ArrayList<MedicalStaff> (10);

		for(int i=30;i<40;i++) {
			ArrayList<Jobs> ms= new ArrayList<Jobs>();
			MedicalStaff listMedicalstaff= new MedicalStaff(i, ms,3);
			medicalStaff.add(listMedicalstaff);
			nursesQ3.add(listMedicalstaff);
		}


		allRequestsList.sort(Jobs.SORT_BY_QUALIFICATION);
		// clients
		for(int i=0; i<allRequestsList.size();i++ ) { // initialize schedulling	& falta incluir las working hours
			if((allRequestsList.get(i).getReqQualification()>0 && allRequestsList.get(i).getTotalPeople()<0) ) {// hard constraint
				switch (allRequestsList.get(i).getReqQualification()) {
				case 1:
					insertJobs(relationship,allRequestsList,nursesQ1,i);
					// Perform some action for "start" command
					break;
				case 2:
					insertJobs(relationship,allRequestsList,nursesQ2,i);
					// Perform some action for "stop" command
					break;
				case 3:
					insertJobs(relationship,allRequestsList,nursesQ3,i);
					// Perform some action for "restart" command
					break;
				default:
					System.out.println("Invalid command. Try again.");
				}


			}
		}

		computeWorkingHours();

		for(MedicalStaff ms:	medicalStaff ) {
			if(ms.getAvailability().size()>0) {
				System.out.println(ms.toString());
			}
		}

		for(MedicalStaff ms:	nursesQ1 ) {
			if(ms.getAvailability().size()>0) {
				System.out.println(ms.toString());
			}
		}


		for(MedicalStaff ms:	nursesQ2 ) {
			if(ms.getAvailability().size()>0) {
				System.out.println(ms.toString());
			}
		}


		for(MedicalStaff ms:	nursesQ3 ) {
			if(ms.getAvailability().size()>0) {
				System.out.println(ms.toString());
			}
		}


	}



	private void computeWorkingHours() {
		for(MedicalStaff ms: nursesQ1) {
			completeStaff.put(ms.getId(), ms);
			ms.computeWorkingHours();
		}

		for(MedicalStaff ms: nursesQ2) {
			completeStaff.put(ms.getId(), ms);
			ms.computeWorkingHours();
		}

		for(MedicalStaff ms: nursesQ3) {
			completeStaff.put(ms.getId(), ms);
			ms.computeWorkingHours();
		}

		for(MedicalStaff ms: medicalStaff) {
			ms.computeWorkingHours();
		}

	}


}
