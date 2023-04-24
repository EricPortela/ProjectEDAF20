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
	 * Modify it to fit your environment and then use this string when connecting to
	 * your database!
	 */
	private static final String jdbcString = "jdbc:mysql://puccini.cs.lth.se/";

	// For use with MySQL or PostgreSQL
	private static final String jdbcUsername = "hbg09";
	private static final String jdbcPassword = "olz278gx";

	private Connection conn;

	public void connect() {
		// Connect to database here
		try {
			conn = DriverManager.getConnection(jdbcString + jdbcUsername, jdbcUsername, jdbcPassword);
		} catch (SQLException e) {
			System.out.println("Failed to connect to database...");
			System.err.println(e);
			e.printStackTrace();
		}
	}


	/**
	 * Returns a JSON string containing all the customers' names and addresses. Retrieves this information
	 * from the database by executing a SQL query that selects the customerName and address columns from the Customers table.
	 * @param req the HTTP request object
	 * @param res the HTTP response object
	 * @return a JSON string containing all the customers' names and addresses
	 */

	public String getCustomers(Request req, Response res) {

		String sql = "SELECT customerName AS 'name', address FROM Customers";

		try (PreparedStatement ps = conn.prepareStatement(sql)) {

			ResultSet rs = ps.executeQuery();

			String json = Jsonizer.toJson(rs, "customers");

			return json;
		}

		catch (SQLException ex) {
			System.out.println("Failed to execute query to get customers...");
			ex.printStackTrace();
		}

		return "{}";
	}

	/**
	 * Retrieves information about raw materials from the database and returns it as a JSON string.
	 * The method executes a SQL query to retrieve the name, amount, and unit of all raw materials
	 * stored in the Ingredients table of the database. The results are formatted as a JSON string
	 * with keys "name", "amount", and "unit" corresponding to the respective columns in the database.
	 * @param req the HTTP request object
	 * @param res the HTTP response object
	 * @return a JSON string representing the raw materials information, or "{}" if there was an error
	 */

	public String getRawMaterials(Request req, Response res) {

		String sql = "SELECT ingredientName AS 'name', amountInStorage AS 'amount', unit FROM Ingredients;";

		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ResultSet rs = ps.executeQuery();

			String json = Jsonizer.toJson(rs, "raw-materials");

			return json;

		} catch (SQLException ex) {
			System.out.println("Failed to execute query to get raw materials...");
			ex.printStackTrace();
		}

		return "{}";
	}


	/**
	 * Returns a JSON string containing a list of all available cookies.
	 * The list is obtained from the database table "Recipes" using a SQL query.
	 * The returned JSON string has a root element "cookies".
	 * @param req The HTTP request object
	 * @param res The HTTP response object
	 * @return A JSON string containing a list of all available cookies
	 */

	public String getCookies(Request req, Response res) {
		String sql = "SELECT recipeName AS 'name' FROM Recipes;";

		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ResultSet rs = ps.executeQuery();

			String json = Jsonizer.toJson(rs, "cookies");

			return json;

		} catch (SQLException ex) {
			System.out.println("Failed to execute query to get cookies...");
			ex.printStackTrace();
		}

		return "{\"cookies\":[]}";
	}


	/**
	 * Returns a JSON string representing all the recipes in the system, along with their respective ingredients and amounts.
	 * @param req The HTTP request object.
	 * @param res The HTTP response object.
	 * @return A JSON string representing all the recipes in the system, or an empty string if there was an error executing the query.
	 */

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

		} catch (SQLException ex) {
			System.out.println("Failed to execute query to get recipes...");
			ex.printStackTrace();
		}

		return "";
	}


	/**
	 * Retrieves a list of pallets from the database based on query parameters and returns them as JSON.
	 * @param req The HTTP request containing the query parameters.
	 * @param res The HTTP response to return.
	 * @return A JSON representation of the retrieved pallets.
	 */

	public String getPallets(Request req, Response res) {

		String viewName = "pallet_temp";

		//STEP 1.
		//Create temporary view, containing ALL necessary attributes for this method (Attributes: id, cookie, production_date, customer, blocked)
		dropHelpView(viewName); //drop view, if exists
		createHelpView(viewName);

		//STEP 2.
		//Query the newly created view based on query-params
		String sql = "SELECT * FROM " + viewName;
		ArrayList<String> values = new ArrayList<String>();

		int count = 0;
		if (req.queryParams("blocked") != null) {
			count ++;
			if (count > 1) {
				sql += " AND blocked=?";
				values.add(req.queryParams("blocked"));
			} else {
				sql += " WHERE blocked=?";
				values.add(req.queryParams("blocked"));
			}
		}

		if (req.queryParams("from") != null) {
			count++;
			if (count > 1) {
				sql += " AND DATE(production_date)>=?";
				values.add(req.queryParams("from"));
			} else {
				sql += " WHERE DATE(production_date)>=?";
				values.add(req.queryParams("from"));
			}
		}

		if (req.queryParams("to") != null) {
			count++;
			if (count > 1) {
				sql += " AND DATE(production_date)<=?";
				values.add(req.queryParams("to"));
			} else {
				sql += " WHERE DATE(production_date)<=?";
				values.add(req.queryParams("to"));
			}
		}

		if (req.queryParams("cookie") != null) {
			count++;
			if (count > 1) {
				sql += " AND cookie=?";
				values.add(req.queryParams("cookie"));
			} else {
				sql += " WHERE cookie=?";
				values.add(req.queryParams("cookie"));
			}
		}
		

		//STEP 3.
		//Create prepared statement based on query-params
		// add "AND" keyword between all sql "where" commands
		try (PreparedStatement ps = conn.prepareStatement(sql)) {

			for (int i = 0; i < values.size(); i++) {
				ps.setString(i + 1, values.get(i));
			}

			ResultSet rs = ps.executeQuery();

			String json = Jsonizer.toJson(rs, "pallets");

			return json;

		} catch (SQLException ex) {
			System.out.println("Failed to execute query to get pallets...");
			ex.printStackTrace();
		}
		return "{\"pallets\":[]}";
	}

	//USED in getPallets()
	private boolean createHelpView(String viewName) {
		String sql = "CREATE VIEW " + viewName + " AS "
					+ "SELECT Pallets.palletNo AS 'id', Pallets.recipeName AS 'cookie', Pallets.producedDateTime AS 'production_date', IF(Pallets.blocked, 'yes', 'no') AS blocked, Orders.customerName AS 'customer' "
					+ "FROM Pallets LEFT JOIN Deliveries "
					+ "ON Pallets.palletNo = Deliveries.palletNo "
					+ "LEFT JOIN Orders "
					+ "ON Deliveries.orderId = Orders.orderId "
					+ "ORDER BY production_date DESC";

		try (PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.executeUpdate();
			return true;

		} catch(SQLException ex) {
			System.out.println("Failed to create temporary view to be used in getPallet()-method...");
			ex.printStackTrace();
			return false;
		}
	}

	//USED in getPallets()
	private boolean dropHelpView(String viewName) {
		String sql = "DROP VIEW IF EXISTS " + viewName;

		try (PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.executeUpdate();
			return true;

		} catch(SQLException ex) {
			System.out.println("Failed to create temporary view to be used in getPallet()-method...");
			ex.printStackTrace();
			return false;
		}
	}


	/**
	 * This method handles a reset request to truncate all tables in the database and reset foreign key checks.
	 * It executes a list of SQL queries obtained from the ResetDB class and returns a JSON response indicating the
	 * success status of the operation.
	 * @param req The request object containing the incoming HTTP request data.
	 * @param res The response object containing the outgoing HTTP response data.
	 * @return A JSON response string indicating the success status of the reset operation.
	 */

	public String reset(Request req, Response res) {

		ArrayList<String> sqlQueries = ResetDB.getResetQueries(); //SEE ResetDB.java class!

		for (String sql: sqlQueries) {
			try (PreparedStatement ps = conn.prepareStatement(sql)) {

				if (sql.contains("SET")) {
					ps.execute();
				} else {
					ps.executeUpdate();
				}

			} catch (SQLException e) {
				System.out.println("\n\n\n");
				System.out.println("Failed executing query: ");
				System.out.println(sql);
				e.printStackTrace();
			}
		}

		return "{\"status\": \"ok\" }";
	}


	/**
	 * Creates a new pallet based on the specified cookie.
	 * @param req The HTTP request.
	 * @param res The HTTP response.
	 * @return A JSON object containing the status of the request and the ID of the new pallet, or an error message.
	 */

	public String createPallet(Request req, Response res) {

		//STEP 1
		//Create a map containing {ingredientName: amountIngredient}
		String cookieName = req.queryParams("cookie");
		Map<String, Integer> map = getCookiesRecipe(cookieName);

		if (map == null) {
			return "{\"status \":\"ok\", \"id: \" \"unknown cookie\"}";
		}

		//STEP 2
		//Iterate over each ingredient and update storage/value of ingredient
		for (Map.Entry<String, Integer> e : map.entrySet()) {

			String ingredient = e.getKey();
			int amountUsed = e.getValue() * 54;

			String updateIngredient = "UPDATE Ingredients set amountInStorage = amountInStorage- ?"
					+ " WHERE ingredientName = ?";

			try (PreparedStatement ps1 = conn.prepareStatement(updateIngredient)) {
				// Initiera transaktion genom att sätta autoCommit till false
				conn.setAutoCommit(false);

				ps1.setInt(1, amountUsed);
				ps1.setString(2, ingredient);
				ps1.executeUpdate();

			} catch (SQLException ex1) {
				System.out.println("Failed updating some ingredient values...");
				ex1.printStackTrace();

				try {
					conn.rollback();
					conn.setAutoCommit(true);
				} catch (SQLException ex2) {
					ex2.printStackTrace();
				}

				return " {\"status \":\"ok\", \"id: \" error}";
			}
		}

		//STEP 3
		//Insert the new pallet
		String sql = "INSERT into Pallets(recipeName, producedDateTime, blocked) values (?, NOW(), ?)";
		PreparedStatement ps = null;

		try {
			ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
			ps.setString(1, cookieName);
			// ps.setString(2, "NOW()");
			ps.setBoolean(2, false);

			ps.executeUpdate();

			ResultSet rs = ps.getGeneratedKeys();

			if (rs.next()) {
				int idx = rs.getInt(1);

				// Transaktioner lyckad ==> committa
				conn.commit();

				return " {\"status \":\"ok\", \"id: \" " + idx + "}";
			}

		} catch (SQLException ex3) {
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

	// USED in createPallet()
	// Returns a map containing the ingredients of the requested cookie
	// Key = Ingredient Name, Value = Amount Of Ingredient
	private Map<String, Integer> getCookiesRecipe(String cookieName) {
		Map<String, Integer> map = new HashMap<>();

		String sql = "SELECT * FROM RecipeIngredient WHERE recipeName = ?";

		try (PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setString(1, cookieName);
			ResultSet rs = ps.executeQuery();

			while (rs.next()) {
				map.put(rs.getString(2), rs.getInt(3));
			}

			return map;

		} catch (SQLException ex) {
			System.out.println("Failed to execute query to get cookies recipe...");
			ex.printStackTrace();
		}

		return null;
	}






	//### TEST METHODS


	//## TEST VERSION of createPallet()
	// public String createPalettTest(String cookieName) {
	// 	Map<String, Integer> map = getCookiesRecipe(cookieName);

	// 	if (map == null) {
	// 		return "{\"status \":\"ok\", \"id: \" \"unknown cookie\"}";
	// 	}

	// 	for (Map.Entry<String, Integer> e : map.entrySet()) {

	// 		String ingredient = e.getKey();
	// 		int amountUsed = e.getValue() * 54;

	// 		String updateIngredient = "UPDATE Ingredients set amountInStorage = amountInStorage- ?"
	// 				+ " WHERE ingredientName = ?";

	// 		try (PreparedStatement ps1 = conn.prepareStatement(updateIngredient)) {
	// 			// Initiera transaktion genom att sätta autoCommit till false
	// 			conn.setAutoCommit(false);

	// 			ps1.setInt(1, amountUsed);
	// 			ps1.setString(2, ingredient);
	// 			ps1.executeUpdate();

	// 		} catch (SQLException ex1) {
	// 			System.out.println("Failed updating some ingredient values...");
	// 			ex1.printStackTrace();

	// 			try {
	// 				conn.rollback();
	// 				conn.setAutoCommit(true);
	// 			} catch (SQLException ex2) {
	// 				ex2.printStackTrace();
	// 			}

	// 			return " {\"status \":\"ok\", \"id: \" error}";
	// 		}
	// 	}

	// 	String sql = "INSERT into Pallets(recipeName, producedDateTime, blocked) values (?, NOW(), ?)";
	// 	PreparedStatement ps = null;

	// 	try {

	// 		ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
	// 		ps.setString(1, cookieName);
	// 		// ps.setString(2, "NOW()");
	// 		ps.setBoolean(2, false);

	// 		ps.executeUpdate();

	// 		ResultSet rs = ps.getGeneratedKeys();

	// 		if (rs.next()) {
	// 			int idx = rs.getInt(1);

	// 			// Transaktioner lyckad ==> committa
	// 			conn.commit();

	// 			System.out.println("{\"status \":\"ok\", \"id: \" " + idx + "}");

	// 			return " {\"status \":\"ok\", \"id: \" " + idx + "}";
	// 		}

	// 	} catch (SQLException ex3) {
	// 		System.out.println("Failed to execute query to create pallet...");
	// 		ex3.printStackTrace();

	// 		try {
	// 			conn.rollback();
	// 		} catch (SQLException ex4) {
	// 			System.out.println("Failed rolling back transaction!");
	// 			ex4.printStackTrace();
	// 		}
	// 	}

	// 	return " {\"status \":\"ok\", \"id: \" error}";
	// }




	//### THIS RESET METHOD READ THE SQL-SCRIPT AND EXECUTE IT THEREAFTER
	// public String reset(Request req, Response res) {

	// 	String sql = "";

	// 	try {
	// 		FileReader in = new FileReader("../ProjectEDAF20/initial-data.sql");
	// 		BufferedReader br = new BufferedReader(in);
	// 		String line;
	// 		try {
	// 			while ((line = br.readLine()) != null) {

	// 				sql += line;

	// 				if (line.length() != 0 && line.charAt(line.length() - 1) == ';') {

	// 					try (PreparedStatement ps = conn.prepareStatement(sql)) {

	// 						if (sql.contains("SET")) {
	// 							ps.execute();
	// 						} else {
	// 							ps.executeUpdate();
	// 						}

	// 						sql = "";

	// 					} catch (SQLException e) {
	// 						e.printStackTrace();
	// 					}
	// 				}
	// 			}
	// 		} catch (IOException e) {
	// 			e.printStackTrace();
	// 		}
	// 	} catch (FileNotFoundException e) {
	// 		System.out.println("File not found");
	// 	}

	// 	return "{\"status\": \"ok\" }";
	// }




	//### NOT FULLY FUNCTIONING RESET
	/*
	 * public String reset(Request req, Response res) {
	 * 
	 * // Turn off foreign key checks
	 * try(Statement stmt = conn.createStatement()){
	 * stmt.execute("SET FOREIGN_KEY_CHECKS=0");
	 * stmt.close();
	 * }
	 * catch(SQLException e){e.printStackTrace();}
	 * 
	 * // Truncate tables
	 * String[] tables = {"Recipes", "Ingredients", "RecipeIngredient",
	 * "IngredientDeliveries",
	 * "Pallets", "Customers", "Orders", "OrderRecipes", "Deliveries"};
	 * 
	 * for(int i = 0; i < tables.length; i++){
	 * String sql = "TRUNCATE " + tables[i] + ";";
	 * try (PreparedStatement ps = conn.prepareStatement(sql)) {
	 * ps.executeUpdate();
	 * } catch(SQLException e) {
	 * e.printStackTrace();
	 * }
	 * }
	 * 
	 * // Turn on foreign key checks
	 * try(Statement stmt = conn.createStatement()){
	 * stmt.execute("SET FOREIGN_KEY_CHECKS=1");
	 * stmt.close();
	 * }
	 * catch(SQLException e){e.printStackTrace();}
	 * 
	 * return "{}";
	 * }
	 */


}

