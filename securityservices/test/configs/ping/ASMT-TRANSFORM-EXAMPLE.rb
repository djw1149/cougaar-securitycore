=begin experiment

#
# This experiment demonstrates how to build a society configuration
# file by using ACME experiments.
#

name: ASMT-PING-SAMPLE
description: TransformSociety
script: $CIP/configs/ping/TransformTemplate.rb
parameters:
  - run_count: 1
  - society_file: $CIP/configs/ping/MiniPingSociety.rb
# Use other layout files as needed (one, two or four hosts, or create your own)
#  - layout_file: $CIP/configs/ping/Secure-MiniPing-layout-two-hosts.xml
#  - layout_file: $CIP/configs/ping/Secure-MiniPing-layout.xml
  - layout_file: $CIP/configs/ping/Secure-MiniPing-layout-one-host.xml
  
  - rules:
    - $CIP/csmart/config/rules/isat/community_plugin.rule
    - $CIP/csmart/config/rules/isat/default_servlets.rule
    - $CIP/csmart/config/rules/isat/http_port.rule
    - $CIP/csmart/config/rules/isat/jvm_props.rule
    - $CIP/csmart/config/rules/isat/logging_config_servlet.rule
    - $CIP/csmart/config/rules/isat/nameserver.rule
    - $CIP/csmart/config/rules/isat/root_mobility_plugin.rule
    - $CIP/csmart/config/rules/isat/set_rogue_thread.rule
    - $CIP/csmart/config/rules/isat/show_jars.rule
    - $CIP/csmart/config/rules/isat/tic_env.rule
    - $CIP/csmart/config/rules/isat/time_control.rule
    - $CIP/csmart/config/rules/isat/web_timeout.rule
    - $CIP/csmart/config/rules/yp
# ######################################################
# Security rules
    - $CIP/csmart/config/rules/security
    - $CIP/csmart/config/rules/security/mts/loopback_protocol.rule
#   - $CIP/csmart/config/rules/security/mts/https_mts.rule
    - $CIP/csmart/config/rules/security/mts/sslRMI.rule
    - $CIP/csmart/config/rules/security/naming
    - ./vm.rules
  - community_rules:
    - $CIP/csmart/config/rules/security/communities

include_scripts:
  - script: setup_ping.rb
# ######################################################

=end

# Fix path problems on cygwin
os=`uname`
if os =~ /CYGWIN/
  CIP=`cygpath -u #{ENV['CIP']}`.strip
else
  CIP = ENV['CIP']
end

$:.unshift File.join(CIP, 'csmart', 'acme_scripting', 'src', 'lib')
$:.unshift File.join(CIP, 'csmart', 'acme_service', 'src', 'redist')
# Below is the path when using open-source ACME
$:.unshift File.join(CIP, 'acme', 'acme_scripting',  'src', 'lib')
$:.unshift File.join(CIP, 'acme', 'acme_service', 'src', 'redist')

require 'cougaar/scripting'
Cougaar::ExperimentDefinition.register(__FILE__)
