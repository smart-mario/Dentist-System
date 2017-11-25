package mysql.query;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import mysql.MySQLAccess;

public class AddressQuery extends QuerySQL {
	
	public AddressQuery(MySQLAccess access) {
		super(access);
	}
	
	public ResultSet get(int houseNumber, String postCode) throws Exception {
		PreparedStatement get = connect.prepareStatement("SELECT HouseNumer Postcode FROM Address "
				+ "HouseNumber = " +houseNumber+ " and Postcode = " +postCode+ ";");
		return resultSet;
		
		
		
	}
	public void add(int houseNumber, String postCode, String streetName, String districtName, String cityName) {
		
		try {
			PreparedStatement insert = connect.prepareStatement("INSERT INTO Address VALUES ("
					+houseNumber+ ", '" +postCode+ "', '" +streetName+ "', '" +districtName+ "', '" +cityName+ "');");
			
			insert.executeUpdate();
			
		} catch(Exception e) {System.out.println(e);}
	}
	
	
}
