=begin experiment

name: MOAS-II-AS-Save
description: MOAS-II-AS save pre-Stage5
script: $CIP/csmart/scripts/definitions/BaselineTemplate-ExtOplan.rb
parameters:
  - run_count: 1
  - society_file: $CIP/csmart/config/societies/ua/full-tc20-232a703v.plugins.rb
# for 2 rack
  - layout_file: $CIP/operator/layouts/04-OP-layout.xml
  - archive_dir: $CIP/Logs
  
  - rules:
    - $CIP/csmart/config/rules/isat
    - $CIP/csmart/config/rules/isat/uc3_nosec
    - $CIP/csmart/config/rules/yp
    - $CIP/csmart/config/rules/logistics
    - $CIP/csmart/config/rules/assessment
# Robustnes
#    - $CIP/csmart/config/rules/robustness/manager.rule
#    - $CIP/csmart/config/rules/robustness/uc1
#    - $CIP/csmart/config/rules/robustness/uc7
#    - $CIP/csmart/config/rules/robustness/uc9
#    - $CIP/csmart/config/rules/robustness/UC3
#    - $CIP/csmart/config/rules/metrics/basic
#    - $CIP/csmart/config/rules/metrics/sensors
#    - $CIP/csmart/config/rules/metrics/serialization/metrics-only-serialization.rule
#    - $CIP/csmart/config/rules/metrics/rss/tic

# Security rules
    - $CIP/csmart/config/rules/security
    - $CIP/csmart/lib/security/rules
    - $CIP/csmart/config/rules/security/mop
    - $CIP/csmart/config/rules/security/testCollectData/ServiceContractPlugin.rule
   # Redundant CA and persistence managers
    - $CIP/csmart/config/rules/security/robustness
#    Enable the rules below only when the layout does not include the
#     redundant CA and PM facets
#    - $CIP/csmart/config/rules/security/redundancy
   # Enable the rules below when the redundant PMs are enabled but not the redundant CAs
#    - $CIP/csmart/config/rules/security/redundancy/add_redundant_pm_facet.rule
#    - $CIP/csmart/config/rules/security/redundancy/adjust_memory.rule
#    - $CIP/csmart/config/rules/security/robustness/redundant_persistence_mgrs.rule

# Robustness rules
#    - $CIP/csmart/config/rules/robustness/manager.rule
#    - $CIP/csmart/config/rules/robustness/uc1/aruc1.rule
#    - $CIP/csmart/config/rules/robustness/uc1/debug/mic.rule
#    - $CIP/csmart/config/rules/robustness/uc1/tuning/collect_stats.rule
#    - $CIP/csmart/config/rules/robustness/uc9/deconfliction.rule

  - community_rules:
    - $CIP/csmart/config/rules/security/communities
#    - $CIP/csmart/config/rules/robustness/communities

include_scripts:
  - script: $CIP/csmart/lib/isat/clearPnLogs.rb
  - script: $CIP/csmart/lib/isat/network_shaping.rb
  - script: $CIP/csmart/lib/isat/klink_reporting.rb
  - script: $CIP/csmart/lib/robustness/objs/deconfliction.rb
  - script: $CIP/csmart/lib/isat/datagrabber_include.rb
  - script: $CIP/csmart/assessment/assess/inbound_aggagent_include.rb
  - script: $CIP/csmart/assessment/assess/outofbound_aggagent_include.rb

# Security scripts
  - script: $CIP/csmart/lib/security/scripts/setup_scripting.rb
  - script: $CIP/csmart/lib/security/scripts/setup_userManagement.rb
  - script: $CIP/csmart/lib/security/scripts/log_node_process_info.rb
#  - script: $CIP/csmart/lib/security/scripts/setup_society_1000_ua.rb
  - script: $CIP/csmart/lib/security/scripts/check_wp.rb
  - script: $CIP/csmart/lib/security/scripts/check_report_chain_ready.rb
  - script: $CIP/csmart/lib/security/scripts/revoke_agent_and_node_cert.rb
  - script: $CIP/csmart/lib/security/scripts/stress_security_uc1.rb
  - script: $CIP/csmart/lib/security/scripts/stress_security_uc3.rb
  - script: $CIP/csmart/lib/security/scripts/stress_security_uc4.rb
  - script: $CIP/csmart/lib/security/scripts/stress_security_uc5.rb
  - script: $CIP/csmart/lib/security/scripts/threatcon_level_change.rb
  - script: $CIP/csmart/lib/security/scripts/invalid_community_request.rb
  - script: $CIP/csmart/lib/security/scripts/check_mop.rb
  - script: $CIP/csmart/lib/security/scripts/parseResults.rb
  - script: $CIP/csmart/lib/security/scripts/saveAcmeEvents.rb
  - script: $CIP/csmart/lib/security/scripts/security_archives.rb
  - script: $CIP/csmart/lib/security/scripts/cleanup_society.rb

#  - script: setup_robustness.rb
#  - script: $CIP/csmart/lib/isat/network_shaping.rb
#  - script: $CIP/csmart/lib/isat/datagrabber_include.rb
#  - script: $CIP/csmart/assessment/assess/inbound_aggagent_include.rb
#  - script: $CIP/csmart/assessment/assess/outofbound_aggagent_include.rb
#  - script: $CIP/csmart/assessment/assess/cnccalc_include.rb

  - script: $CIP/csmart/lib/isat/save_snapshot.rb
    parameters:
      - snapshot_name: $CIP/SAVE-PreStage5.tgz
      - snapshot_location: before_stage_5

=end

require 'cougaar/scripting'
Cougaar::ExperimentDefinition.register(__FILE__)