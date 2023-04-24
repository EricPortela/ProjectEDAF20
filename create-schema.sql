-- Specify database
use hbg09;

-- Delete tables if they exist
set foreign_key_checks = 0;

drop table if exists Recipes;
drop table if exists Ingredients;
drop table if exists RecipeIngredient;
drop table if exists IngredientDeliveries;

drop table if exists Pallets;
drop table if exists Customers;
drop table if exists Orders;
drop table if exists OrderRecipes;
drop table if exists Deliveries;


set foreign_key_checks = 1;

-- Create tables related to recipes and ingredients
CREATE TABLE Recipes(
        recipeName VARCHAR(100) PRIMARY KEY
);

CREATE TABLE Ingredients(
        ingredientName VARCHAR(100) PRIMARY KEY,
        amountInStorage INT,
        unit VARCHAR(100),
);

CREATE TABLE RecipeIngredient(
        recipeName VARCHAR(100) REFERENCES Recipes(recipeName),
        ingredientName VARCHAR(100) REFERENCES Ingredients(ingredientName),
        amount INT,
        PRIMARY KEY(recipeName, ingredientName)
);

CREATE TABLE IngredientDeliveries(
        ingredientDeliveryId INT auto_increment PRIMARY KEY,
        ingredientName VARCHAR(100) REFERENCES Ingredients(ingredientName),
        deliveryDateTime DATETIME,
        amountDelivered INT
);

-- Create tables related to pallets and deliveries
CREATE TABLE Pallets(
        palletNo INT auto_increment PRIMARY KEY,
        recipeName VARCHAR(100) REFERENCES Recipes(recipeName),
        producedDateTime DATETIME,
        blocked BOOLEAN
);

CREATE TABLE Customers(
        customerName VARCHAR(100) PRIMARY KEY,
        address VARCHAR(100),
        phone VARCHAR(100)
);

CREATE TABLE Orders(
        orderId INT auto_increment PRIMARY KEY,
        customerName VARCHAR(100) REFERENCES Customers(customerName),
        deliveryDateTime DATETIME
);

CREATE TABLE OrderRecipes(
        orderId INT REFERENCES Orders(orderId),
        recipeName VARCHAR(100) REFERENCES Recipes(recipeName),
        amountOfPallets INT,
        PRIMARY KEY(orderId, recipeName)
);

CREATE TABLE Deliveries(
        deliveryId INT auto_increment PRIMARY KEY,
        palletNo INT REFERENCES Pallets(palletNo),
        orderId INT REFERENCES Orders(orderId),
        delivered BOOLEAN
);

--STOP