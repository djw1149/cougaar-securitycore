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
  - script: $CIP/csmart/lib/security/scripts/healthcheck.rb
  - script: $CIP/csmart/lib/security/scripts/cleanup_society.rb
    parameters:
      - cleanup_label: transformed_society
  - script: $CIP/csmart/lib/security/scripts/parseResults.rb
# ######################################################
#  - script: $CIP/csmart/lib/robustness/objs/deconfliction.rb
#  - script: network_shaping.rb
#  - script: cnccalc_include.rb
#  - script: standard_kill.rb

=end

CIP = ENV['CIP']
$:.unshift File.join(CIP, 'csmart', 'acme_scripting', 'src', 'lib')
$:.unshift File.join(CIP, 'csmart', 'acme_service', 'src', 'redist')
$:.unshift File.join(CIP, 'csmart', 'lib')

if File.exist?("#{CIP}/acme")
  # Below is the path when using open-source ACME
  $:.unshift File.join(CIP, 'acme', 'acme_scripting',  'src', 'lib')
  $:.unshift File.join(CIP, 'acme', 'acme_service', 'src', 'redist')
  require 'cougaar/scripting'
  require 'security/actions/cleanup_society'
else
  require 'cougaar/scripting'
end

Cougaar::ExperimentDefinition.register(__FILE__)
