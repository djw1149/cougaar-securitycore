# MySQL dump 8.16
#
# Host: localhost    Database: tempcopy
#--------------------------------------------------------
# Server version	3.23.43-nt

#
# Dumping data for table 'lib_mod_recipe'
#

LOCK TABLES lib_mod_recipe WRITE;
REPLACE INTO lib_mod_recipe (MOD_RECIPE_LIB_ID, NAME, JAVA_CLASS, DESCRIPTION) VALUES ('RECIPE-MnRBootStrapEventPlugin1','MnRBootStrapEventPlugin1','org.cougaar.tools.csmart.recipe.SpecificInsertionRecipe','No description is available');
UNLOCK TABLES;

#
# Dumping data for table 'lib_mod_recipe_arg'
#

LOCK TABLES lib_mod_recipe_arg WRITE;
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-MnRBootStrapEventPlugin1','Class Name',1.000000000000000000000000000000,'org.cougaar.core.security.monitoring.plugin.BootStrapEventPlugin');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-MnRBootStrapEventPlugin1','Component Name',3.000000000000000000000000000000,'org.cougaar.core.security.monitoring.plugin.BootStrapEventPlugin');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-MnRBootStrapEventPlugin1','Component Priority',5.000000000000000000000000000000,'COMPONENT');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-MnRBootStrapEventPlugin1','Number of Arguments',0.000000000000000000000000000000,'3');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-MnRBootStrapEventPlugin1','Target Component Selection Query',2.000000000000000000000000000000,'recipeQueryEnclave1SecurityMnRAgent');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-MnRBootStrapEventPlugin1','Type of Insertion',4.000000000000000000000000000000,'plugin');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-MnRBootStrapEventPlugin1','Value 1',6.000000000000000000000000000000,'SecurityMnRManager');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-MnRBootStrapEventPlugin1','Value 2',7.000000000000000000000000000000,'BootStrapSensor');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-MnRBootStrapEventPlugin1','Value 3',8.000000000000000000000000000000,'Enclave1Security-COMM');
UNLOCK TABLES;

