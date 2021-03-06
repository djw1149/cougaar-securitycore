=begin script

include_path: setup_security.rb
description: special initialization for security

=end

require 'security/lib/scripting'
require 'security/actions/buildUserFiles'

insert_before :setup_run do
  do_action "BuildConfigJarFiles"
  do_action "BuildUserFiles"
end
