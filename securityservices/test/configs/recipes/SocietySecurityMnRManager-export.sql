# MySQL dump 8.16
#
# Host: localhost    Database: tempcopy
#--------------------------------------------------------
# Server version	3.23.44-nt

#
# Dumping data for table 'lib_mod_recipe'
#

LOCK TABLES lib_mod_recipe WRITE;
REPLACE INTO lib_mod_recipe (MOD_RECIPE_LIB_ID, NAME, JAVA_CLASS, DESCRIPTION) VALUES ('RECIPE-SocietySecurityMnRManager','SocietySecurityMnRManager','org.cougaar.tools.csmart.recipe.AgentInsertionRecipe','No description available');
UNLOCK TABLES;

#
# Dumping data for table 'lib_mod_recipe_arg'
#

LOCK TABLES lib_mod_recipe_arg WRITE;
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-SocietySecurityMnRManager','Agent Names',2.000000000000000000000000000000,'SocietySecurityMnRManager');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-SocietySecurityMnRManager','Alternate Type Identification',1.000000000000000000000000000000,'SocietySecurityMnRManager');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-SocietySecurityMnRManager','Asset Class',5.000000000000000000000000000000,'MilitaryOrganization');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-SocietySecurityMnRManager','Class Name',0.000000000000000000000000000000,'org.cougaar.core.agent.ClusterImpl');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-SocietySecurityMnRManager','Include Item Identification PG',8.000000000000000000000000000000,'true');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-SocietySecurityMnRManager','Include Org Asset',3.000000000000000000000000000000,'true');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-SocietySecurityMnRManager','Nomenclature',6.000000000000000000000000000000,'UTC/RTOrg');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-SocietySecurityMnRManager','Number of Relationships',4.000000000000000000000000000000,'0');
REPLACE INTO lib_mod_recipe_arg (MOD_RECIPE_LIB_ID, ARG_NAME, ARG_ORDER, ARG_VALUE) VALUES ('RECIPE-SocietySecurityMnRManager','Type Identification',7.000000000000000000000000000000,'UTC/RTOrg');
UNLOCK TABLES;

