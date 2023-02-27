import java.util.ArrayList;
import java.util.Comparator;

public class MedicalStaff {
	private int id;
	private int qualification;
	private ArrayList<Jobs> availability;
	private double working_hours;


	public MedicalStaff(int id, ArrayList<Jobs> ms, int qualification) {
		this.id=id;
		this.availability =ms;
		this.qualification=qualification;
	}


	// getters
	public int getId() {return id;}
	public int getQualification() {return qualification;}
	public ArrayList<Jobs> getAvailability() {return availability;}
	public double getWorking_hours() {return working_hours;}
	
	// setters
	public void setWorking_hours(double w) {this.working_hours=w;}
	
	public static Comparator<MedicalStaff> jobListSize = new Comparator<MedicalStaff>() {
		@Override
		public int compare(MedicalStaff o1, MedicalStaff o2) {
			if (o1.getAvailability().size()> o2.getAvailability().size())
				return 1;
			if (o1.getAvailability().size() < o2.getAvailability().size())
				return -1;
			return 0;
		}
	};
	
	public static Comparator<MedicalStaff> startTimeLastWork = new Comparator<MedicalStaff>() {
		@Override
		public int compare(MedicalStaff o1, MedicalStaff o2) {
			if (o1.getAvailability().get(o1.getAvailability().size()-1).getStartTime()> o2.getAvailability().get(o1.getAvailability().size()-1).getStartTime())
				return 1;
			if (o1.getAvailability().get(o1.getAvailability().size()-1).getStartTime()< o2.getAvailability().get(o1.getAvailability().size()-1).getStartTime())
				return -1;
			return 0;
		}
	};
	
	
	public static Comparator<MedicalStaff> startTimefirstWork = new Comparator<MedicalStaff>() {
		@Override
		public int compare(MedicalStaff o1, MedicalStaff o2) {
			if (o1.getAvailability().get(0).getStartTime()> o2.getAvailability().get(0).getStartTime())
				return 1;
			if (o1.getAvailability().get(0).getStartTime()< o2.getAvailability().get(0).getStartTime())
				return -1;
			return 0;
		}
	};


	public void computeWorkingHours() {
		Double w=0.0;
		for(Jobs j:this.availability) {
			w+=j.getReqTime();
		}
		this.working_hours=w;
	}


	public String toString() {
		String s = "";
		s = s.concat("\n ID: " + this.id + " Q "+ this.qualification+ " workingTime " + this.working_hours);
		for(Jobs j: this.availability) {
			s = s.concat("\n" + j.getSubJobKey() + " Q: "+j.getReqQualification()+ " StartTW "+ j.getStartTime() + " EndTW "+ j.getEndTime() + " serviceTime " +j.getReqTime() + " Total people "+ j.getTotalPeople());
		}
		return s;
	}


}
