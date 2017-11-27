package mysql.query;

import java.sql.Time;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import calendar.Appointment;
import mysql.MySQLAccess;
import sun.security.util.PropertyExpander;

public class AppointmentQuery extends QuerySQL {

	public AppointmentQuery(MySQLAccess access) {
		super(access);
	}
	public int appointmentToPatientID(String appDate, String startTime, String partner) throws Exception{
		String query = "SELECT PatientID FROM Appointments WHERE (AppointmentData =" + appDate + " AND StartTime =" +startTime +" AND Partner =" + partner + "; ";
	 	preparedStatement =  prepareStatement(query); 
	    resultSet = preparedStatement.executeQuery(query);	
		int patientID = 0;
		while(resultSet.next()) {
	        	patientID = resultSet.getInt(1);				
		}
		close();
		return patientID;				
	}
	public boolean isValidTimeSlot(HashMap<String, Object> map) throws Exception {
		String appDate = (String) map.get("AppointmentDate");
		String startTime = (String) map.get("StartTime");
		String partner = (String) map.get("Partner");
		String endTime = (String) map.get("EndTime");
		
		preparedStatement = prepareStatement("SELECT * FROM Appointments WHERE "
				+ "AppointmentDate = (SELECT STR_TO_DATE(?, '%Y-%m-%d')) AND Partner = ? AND "
				+ "((StartTime >= (SELECT STR_TO_DATE(?, '%h:%i %p')) AND StartTime <= (SELECT STR_TO_DATE(?, '%h:%i %p'))) OR "
				+ "(EndTime >= (SELECT STR_TO_DATE(?, '%h:%i %p')) AND EndTime <= (SELECT STR_TO_DATE(?, '%h:%i %p'))));"); 
	        Calendar cal = Calendar.getInstance();
		int day = cal.get(Calendar.DAY_OF_MONTH);
		int mon = cal.get(Calendar.MONTH);
		int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
		int[][] publicHolidays = new int[][] {{2,1},{14,4},{17,4},{1,5},{29,5},{28,8},{25,12},{26,12}};
		for (int row = 0; row < 8; row++) {
			if(day == publicHolidays[row][0] && mon-1 == publicHolidays[row][1]) {
				appDate = null;
			}
		}
		if(dayOfWeek == 1 || dayOfWeek == 7) {
			appDate = null;
		}
		
		preparedStatement.setString(1, appDate);
		preparedStatement.setString(2, partner);
		preparedStatement.setString(3, startTime);
		preparedStatement.setString(4, startTime);
		preparedStatement.setString(5, endTime);
		preparedStatement.setString(6, endTime);
		
		resultSet = preparedStatement.executeQuery();
		
		boolean bool = !resultSet.next();
		
		close();
		
		return bool;
	}

	public Appointment[] get(Date date) throws Exception {
		preparedStatement = prepareStatement(
				"SELECT StartTime, Partner, EndTime, PatientID "
				+ "FROM Appointments WHERE AppointmentDate = ?;");
		preparedStatement.setDate(1, new java.sql.Date(date.getTime()));
		resultSet = preparedStatement.executeQuery();
		
		int rows = getRowCount(resultSet);
		Appointment[] appointments = new Appointment[rows];
		
		while (resultSet.next()) {
			int currRow = resultSet.getRow() - 1;
			
			Time startTime = resultSet.getTime(1);
			String partner = resultSet.getString(2);
			Time endTime = resultSet.getTime(3);
			int patientID = resultSet.getInt(4);

			appointments[currRow] = new Appointment(
					createDate(date, startTime), createDate(date, endTime), 
					partner, patientID);
		}
		
		close();
		
		return appointments;
	}

	public void add(HashMap<String, Object> map) throws Exception {
		String appDate = (String) map.get("AppointmentDate");
		String startTime = (String) map.get("StartTime");
		String partner = (String) map.get("Partner");
		String endTime = (String) map.get("EndTime");
		int patID = (int) map.get("PatientID");
		
		if (startTime.equals("00:00 PM")) {
			startTime = "12:00 AM";
		}
		
		if (endTime.equals("00:00 PM")) {
			endTime = "12:00 AM";
		}
		
		preparedStatement = prepareStatement("INSERT INTO Appointments (AppointmentDate, StartTime, Partner, EndTime, PatientID, PaidFor) "
				+ "VALUES ((SELECT STR_TO_DATE(?, '%Y-%m-%d')), (SELECT STR_TO_DATE(?, '%h:%i %p')), ?, (SELECT STR_TO_DATE(?, '%h:%i %p')), ?, ?)"); 
		preparedStatement.setString(1, appDate);
		preparedStatement.setString(2, startTime);
		preparedStatement.setString(3, partner);
		preparedStatement.setString(4, endTime);
		preparedStatement.setInt(5, patID);
		preparedStatement.setString(6, "N");
		
		preparedStatement.executeUpdate();
		
		close();
	}

	public void remove(Appointment a) throws Exception {
		preparedStatement = prepareStatement("DELETE FROM Appointments WHERE "
				+ "AppointmentDate = ? AND StartTime = ? AND Partner = ?;");
		preparedStatement.setDate(1, new java.sql.Date(a.getStartDate().getTime()));
		preparedStatement.setTime(2, new java.sql.Time(a.getStartDate().getTime()));
		preparedStatement.setString(3, a.getPartner());
		
    	preparedStatement.executeUpdate();
    	
    	close();
	}
	
	private Date createDate(Date date, Time time) {
		Calendar timeC = Calendar.getInstance(Locale.UK);
		timeC.setTime(time);
		
		Calendar calendar = Calendar.getInstance(Locale.UK);
		calendar.setTime(date);
		calendar.set(Calendar.HOUR_OF_DAY, timeC.get(Calendar.HOUR_OF_DAY));
		calendar.set(Calendar.MINUTE, timeC.get(Calendar.MINUTE));
		calendar.set(Calendar.SECOND, timeC.get(Calendar.SECOND));
		
		return calendar.getTime();
	}
	
	public String[][] get(int patientID) throws Exception {
		preparedStatement = prepareStatement(
				"SELECT AppointmentDate, StartTime, Partner, PaidFor "
						+ "FROM Appointments WHERE PatientID = ? ORDER BY AppointmentDate DESC;");
		preparedStatement.setInt(1, patientID);
		resultSet = preparedStatement.executeQuery();

		int rows = getRowCount(resultSet);
		String[][] apps = new String[rows][4];

		while (resultSet.next()) {
			int currRow = resultSet.getRow() - 1;
			apps[currRow][0] = resultSet.getString(1);
			apps[currRow][1] = resultSet.getString(2);
			apps[currRow][2] = resultSet.getString(3);
            apps[currRow][3] = resultSet.getString(4);
		}

		close();
		
		return apps;
	}
	
	public Appointment[] isPaidFor(int patID) throws Exception {
		
		Date currentDate = new Date();
		
		preparedStatement = prepareStatement("SELECT * FROM Appointments "
				+ "WHERE PatientID = " +patID+ " AND PaidFor = 'N' AND AppointmentDate <= ?;");
		
		preparedStatement.setDate(1, new java.sql.Date(currentDate.getTime()));
		resultSet = preparedStatement.executeQuery();
		
		int rows = getRowCount(resultSet);
		Appointment[] appArray = new Appointment[rows];
		
		while (resultSet.next()) {
			int currRow = resultSet.getRow() - 1;
			
			Date appDate = resultSet.getDate(1);
			Time startTime = resultSet.getTime(2);
			String partner = resultSet.getString(3);
			Time endTime = resultSet.getTime(4);

			appArray[currRow] = new Appointment(
					createDate(appDate, startTime), createDate(appDate, endTime), partner, patID);
			
		}
		
		return appArray;
	}
	
}
