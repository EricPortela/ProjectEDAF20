package krusty;

import spark.Request;
import spark.Response;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import java.util.*;
import java.sql.*;


import static krusty.Jsonizer.toJson;

public class Database {
	/**
	 * Modify it to fit your environment and then use this string when connecting to your database!
	 */
	private static final String jdbcString = "jdbc:mysql://localhost/krusty";

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
                ("jdbc:mysql://puccini.cs.lth.se/" + jdbcUsername, jdbcUsername, jdbcPassword);
        }
        catch (SQLException e) {
			System.out.println("Failed to connect to database...");
            System.err.println(e);
            e.printStackTrace();
        }
	}

	// TODO: Implement and change output in all methods below!

	public String getCustomers(Request req, Response res) {

		String sql = "SELECT name, address FROM Customers";

		try (PreparedStatement ps = conn.prepareStatement(sql)) {

			ResultSet rs = ps.getResultSet();

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

		String sql = "SELECT ingridientName AS 'name', amountInStorage AS 'amount', unit FROM Ingridients;";

		try (PreparedStatement ps = conn.prepareStatement("")) {
			ResultSet rs = ps.getResultSet();

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

		try (PreparedStatement ps = conn.prepareStatement("")) {
			ResultSet rs = ps.getResultSet();

			String json = Jsonizer.toJson(rs, "cookies"); 

			return json;

		} catch(SQLException ex) {
			System.out.println("Failed to execute query to get raw materials...");
			ex.printStackTrace();
		}
		
		return "{\"cookies\":[]}";
	}

	public String getRecipes(Request req, Response res) {
		String sql = "SELECT RecipeIngredient.recipeName as 'cookie', RecipeIngredient.ingredientName as 'raw_material', RecipeIngredient.amount as 'amount' FROM RecipeIngredient LEFT JOIN Ingredients on RecipeIngredient.ingredientName = Ingredients.ingredientName";

		try (PreparedStatement ps = conn.prepareStatement("")) {
			ResultSet rs = ps.getResultSet();

			String json = Jsonizer.toJson(rs, "recipes"); 

			return json;

		} catch(SQLException ex) {
			System.out.println("Failed to execute query to get raw materials...");
			ex.printStackTrace();
		}
		
		return "";
	}

	public String getPallets(Request req, Response res) {
		return "{\"pallets\":[]}";
	}

	public String reset(Request req, Response res) {
		return "{}";
	}

	public String createPallet(Request req, Response res) {
		return "{}";
	}
}
