require 'framework/policy_util'

# CIP is defined in policy_util

module Cougaar
   module Actions 
      class InitDM < Cougaar::Action
        def perform
          run.society.each_enclave { |enclave|
            ::Cougaar.logger.info "Publishing conditional policy to #{enclave} policy domain manager"
            deltaPolicy(enclave, <<END_POLICY)
PolicyPrefix=%InitPolicy/

END_POLICY
          }
        end
      end # InitDM
   end # Actions
end # Cougaar