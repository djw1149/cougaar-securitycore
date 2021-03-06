=begin experiment

name: ASMT-PING-1
description: MOAS
script: $CIP/configs/ping/BaselineTemplate.rb
parameters:
  - run_count: 1
  - society_file: $CIP/configs/ping/MiniPingSociety.rb
#  - layout_file: $CIP/operator/layouts/AS-1K-layout.xml
  - layout_file: $CIP/configs/ping/Secure-MiniPing-layout.xml
  - archive_dir: $CIP/Logs
  
  - rules:
    - $CIP/csmart/config/rules/isat
    - $CIP/csmart/config/rules/yp
#    - $CIP/csmart/config/rules/logistics
# ######################################################
# Security rules
    - $CIP/csmart/config/rules/security
    - $CIP/csmart/lib/security/rules/attack-node

# ######################################################
# Security Use Case Testing Rules
#
    - $CIP/csmart/config/rules/security/mop
    - $CIP/csmart/lib/security/rules/mts_queue_viewer.rule
    - $CIP/csmart/config/rules/security/assessment/

#
# Choosing the MTS Protocol: There are currently four protocols:
# loopback, http, https and rmi/ssl.  (I am not sure how to enable rmi
# on its own yet).  Choose protocols by uncommenting the appropriate
# mts rule.  The naming rule that follows the mts rules is needed
# regardless of which protocols are enabled.
#
    - $CIP/csmart/config/rules/security/mts/loopback_protocol.rule
#    - $CIP/csmart/config/rules/security/mts/http_mts.rule
    - $CIP/csmart/config/rules/security/mts/https_mts.rule
#    - $CIP/csmart/config/rules/security/mts/sslRMI.rule
    - $CIP/csmart/config/rules/security/naming

#
# These rules will involve the network configuration service which is
# not quiet ready yet but is needed for the integration work.
#
#    - $CIP/csmart/config/rules/security/network

#    - $CIP/csmart/config/rules/security/ruleset/base
#    - $CIP/csmart/config/rules/security/ruleset/crypto
#    - $CIP/csmart/config/rules/security/ruleset/jaas
#    - $CIP/csmart/config/rules/security/ruleset/accesscontrol
#    - $CIP/csmart/config/rules/security/ruleset/misc
#    - $CIP/csmart/config/rules/security/ruleset/monitoring
#    - $CIP/csmart/config/rules/security/ruleset/debug
#    - $CIP/csmart/config/rules/security/ruleset/signConfig

#    - $CIP/csmart/lib/security/rules
#    - $CIP/csmart/config/rules/security/mop
#    - $CIP/csmart/config/rules/security/testCollectData

   # ###
   # Redundant CA and persistence managers
#    - $CIP/csmart/config/rules/security/redundancy
#    - $CIP/csmart/config/rules/security/robustness
   # ###
# ######################################################
#    - $CIP/csmart/config/rules/security/robustness
#    - $CIP/csmart/config/rules/robustness
#    - $CIP/csmart/config/rules/robustness/uc1
  - community_rules:
    - $CIP/csmart/config/rules/security/communities
#    - $CIP/csmart/config/rules/robustness/communities

include_scripts:
#  - script: clearPnLogs.rb
  - script: $CIP/csmart/lib/security/scripts/setup_scripting.rb
  - script: setup_ping.rb
# ######################################################
# Security rules
  - script: $CIP/csmart/lib/security/scripts/build_config_jarfiles.rb
  - script: $CIP/csmart/lib/security/scripts/build_policies.rb
  - script: $CIP/csmart/lib/security/scripts/setup_acme_user.rb
  - script: $CIP/csmart/lib/security/scripts/setup_userManagement.rb
    parameters:
      - user_mgr_label: wait_for_initialization
  - script: $CIP/csmart/lib/security/scripts/security_archives.rb
  - script: $CIP/csmart/lib/security/scripts/saveAcmeEvents.rb
  # log_node_process_info should be BEFORE setup_society_ping
  - script: $CIP/csmart/lib/security/scripts/log_node_process_info.rb
  # check_wp.rb should be before setup_society_ping.rb
#  - script: $CIP/csmart/lib/security/scripts/check_wp.rb
  - script: $CIP/csmart/lib/security/scripts/setup_society_ping.rb
  - script: $CIP/csmart/lib/security/scripts/parseResults.rb
  - script: $CIP/csmart/lib/security/scripts/cleanup_society.rb
    parameters:
      - cleanup_label: transformed_society
# ######################################################
#  - script: $CIP/csmart/lib/robustness/objs/deconfliction.rb
#  - script: network_shaping.rb
#  - script: cnccalc_include.rb
#  - script: standard_kill.rb
# ######################################################
# Security Use Case Testing Scripts
#
  # Test that agent revocation works properly. Revoke an agent, then a node.
  # This test always revokes an agent named MessageAttacker.
  - script: $CIP/csmart/lib/security/scripts/revoke_agent_and_node_cert.rb
  # This include script is not doing anything for now.
  - script: $CIP/csmart/lib/security/scripts/stress_security_uc1.rb
  # Check that secure messaging works properly.
  - script: $CIP/csmart/lib/security/scripts/stress_security_uc3.rb
  # Test the SecureConfigFinder. This test will create bad jar files
  # (e.g. jar files with revoked cert, expired cert, bad signature,
  # tampered file...) and check that the files in those jar files are not loaded.
  - script: $CIP/csmart/lib/security/scripts/stress_security_uc4.rb
  # Test the Java Security Manager. A malicious plugin attempts to write
  # a file without appropriate privileges, and the Java security manager should
  # reject the request.
  - script: $CIP/csmart/lib/security/scripts/stress_security_uc5.rb
  # Test threatcon level changes: generate bad logins, wait until the threatcon
  # goes up, verify that the security policy is changed, wait until the threatcon
  # goes down when no login failures are applied, then check the security policy
  # is changed again.
  - script: $CIP/csmart/lib/security/scripts/threatcon_level_change.rb
  # Verify that agent without appropriate privileges cannot perform
  # disallowed community requests.
  - script: $CIP/csmart/lib/security/scripts/invalid_community_request.rb
# MOP computation
  - script: $CIP/csmart/lib/security/scripts/check_mop.rb
    parameters:
      - calculate_mop_label: after_stage_1
      - postCalculate_mop_label: before_stage_2
  # Generate a output file to store the results of the security sanity checks
  # and security stresses. This file should always be included whenever the
  # security services are enabled.
  - script: $CIP/csmart/lib/security/scripts/parseResults.rb


=end

CIP = ENV['CIP']
$:.unshift File.join(CIP, 'csmart', 'acme_scripting', 'src', 'lib')
$:.unshift File.join(CIP, 'csmart', 'acme_service', 'src', 'redist')

require 'cougaar/scripting'
Cougaar::ExperimentDefinition.register(__FILE__)
