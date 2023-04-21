package krusty;

import spark.Request;
import spark.Response;

import java.util.*;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;


import static krusty.Jsonizer.toJson;

public class Database {
	/**
	 * Modify it to fit your environment and then use this string when connecting to your database!
	 */
	private static final String jdbcString = "jdbc:mysql://puccini.cs.lth.se/";

	// For use with MySQL or PostgreSQL
	private static final String jdbcUsername = "hbg09";
	private static final String jdbcPassword = "olz278gx";

	private Connection conn;

	public void connect() {
		// Connect to database here
		try {
        	// Connection strings for included DBMS clients:
        	// [MySQL]       jdbc:mysql://[host]/[database]
        	// [PostgreSQL]  jdbc:postgresql://[host]/[database]
        	// [SQLite]      jdbc:sqlite://[filepath]
        	
        	// Use "jdbc:mysql://puccini.cs.lth.se/" + userName if you using our shared server
        	// If outside, this statement will hang until timeout.
            conn = DriverManager.getConnection 
                (jdbcString + jdbcUsername, jdbcUsername, jdbcPassword);
        }
        catch (SQLException e) {
			System.out.println("Failed to connect to database...");
            System.err.println(e);
            e.printStackTrace();
        }
	}

	// TODO: Implement and change output in all methods below!

	public String getCustomers(Request req, Response res) {

		String sql = "SELECT customerName AS 'name', address FROM Customers";

		try (PreparedStatement ps = conn.prepareStatement(sql)) {

			ResultSet rs = ps.executeQuery();
			//while(rs.next()) System.out.println("hej");

			String json = Jsonizer.toJson(rs, "customers"); 

			return json;
		} 

		catch(SQLException ex) {
			System.out.println("Failed to execute query to get customers...");
			ex.printStackTrace();
		}
		
		return "{}";
	}

	public String getRawMaterials(Request req, Response res) {

		String sql = "SELECT ingredientName AS 'name', amountInStorage AS 'amount', unit FROM Ingredients;";

		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ResultSet rs = ps.executeQuery();

			String json = Jsonizer.toJson(rs, "raw-materials"); 

			return json;

		} catch(SQLException ex) {
			System.out.println("Failed to execute query to get raw materials...");
			ex.printStackTrace();
		}
		
		return "{}";
	}

	public String getCookies(Request req, Response res) {
		String sql = "SELECT recipeName AS 'name' FROM Recipes;";

		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ResultSet rs = ps.executeQuery();

			String json = Jsonizer.toJson(rs, "cookies"); 

			return json;

		} catch(SQLException ex) {
			System.out.println("Failed to execute query to get raw materials...");
			ex.printStackTrace();
		}
		
		return "{\"cookies\":[]}";
	}

	public String getRecipes(Request req, Response res) {
		String sql = "SELECT RecipeIngredient.recipeName as 'cookie', " 
					+ "RecipeIngredient.ingredientName as 'raw_material', "
					+ "RecipeIngredient.amount as 'amount', "
					+ "Ingredients.unit as 'unit' "
					+ "FROM RecipeIngredient LEFT JOIN "
					+ "Ingredients on RecipeIngredient.ingredientName = Ingredients.ingredientName";

		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ResultSet rs = ps.executeQuery();

			String json = Jsonizer.toJson(rs, "recipes");

			return json;

		} catch(SQLException ex) {
			System.out.println("Failed to execute query to get raw materials...");
			ex.printStackTrace();
		}
		
		return "";
	}

	public String getPallets(Request req, Response res) {
		String sql = "SELECT ";

		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ResultSet rs = ps.executeQuery();

			String json = Jsonizer.toJson(rs, "recipes"); 

			return json;

		} catch(SQLException ex) {
			System.out.println("Failed to execute query to get raw materials...");
			ex.printStackTrace();
		}		
		return "{\"pallets\":[]}";
	}

	public String reset(Request req, Response res) {

        String sql = "";

        try{
            FileReader in = new FileReader("ProjectEDAF20/initial-data.sql");
            BufferedReader br = new BufferedReader(in);
            String line;
            try{
                while((line = br.readLine()) != null){
                    sql += line;
                }
            }
            catch(IOException e){e.printStackTrace();}
        }
        catch(FileNotFoundException e){System.out.println("File not found");}

		

        return "{}";
    }

	public String createPallet(Request req, Response res) {

		String cookieName = req.queryParams("cookie");
		Map<String, Integer> map = getCookiesRecipe(cookieName);

		if (map == null) {
			return "{\"status \":\"ok\", \"id: \" \"unknown cookie\"}";
		}

		for (Map.Entry<String, Integer> e: map.entrySet()) {
			
			String ingredient = e.getKey(); 
			int amountUsed = e.getValue() * 54;

			String updateIngredient = "UPDATE Ingredients set amountInStorage = amountInStorage - " + amountUsed
									+ "WHERE ingredientName = ?";

			try (PreparedStatement ps1 = conn.prepareStatement(updateIngredient)) {
				//Initiera transaktion genom att sätta autoCommit till false
				conn.setAutoCommit(false);

				ps1.setString(1, ingredient);
				ps1.executeUpdate();

			} catch(SQLException ex1) {
				System.out.println("Failed updating some ingredient values...");
				ex1.printStackTrace();

				try {
					conn.rollback();
					conn.setAutoCommit(true);
				} catch(SQLException ex2) {
					ex2.printStackTrace();
				}
				
				return " {\"status \":\"ok\", \"id: \" error}";
			}
		}


		String sql = "INSERT into Pallets values(null, ?, ?, ?)";
		try (PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setString(1, cookieName);
			ps.setString(2, "NOW()");
			ps.setBoolean(3, false);
			ps.executeUpdate();

			//Transaktioner lyckad ==> committa
			conn.commit();

			return " {\"status \":\"ok\", \"id: \" " + cookieName + "}";

		} catch(SQLException ex3) {
			System.out.println("Failed to execute query to create pallet...");
			ex3.printStackTrace();

			try {
				conn.rollback();
			} catch (SQLException ex4) {
				System.out.println("Failed rolling back transaction!");
				ex4.printStackTrace();
			}
		}

		return " {\"status \":\"ok\", \"id: \" error}";
	}

	//Returns a map containing the ingredients of the requested cookie
	//Key = Ingredient Name, Value = Amount Of Ingredient
	private Map<String, Integer> getCookiesRecipe(String cookieName){
		Map<String, Integer> map = new HashMap<>();

		String sql = "SELECT * FROM RecipeIngredient WHERE recipeName = ?";

		try(PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setString(0, cookieName);
			ResultSet rs = ps.executeQuery();

			while(rs.next()) {
				map.put(rs.getString(1), rs.getInt(2));
			}

			return map;

		} catch(SQLException ex) {
			System.out.println("Failed to execute query to get cookies recipe...");
			ex.printStackTrace();
		}

		return null;
	}
}
