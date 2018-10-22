# rebar-graph-aws

Builds graph model of AWS infrastructure
# Data Model 


## AwsAccount



| Name | Type | Description |
|------|------|------|
| **account** | string | AWS Account that owns this Account |
| graphEntityType | STRING | |
| graphEntityGroup | STRING | |
| graphUpdateTs | NUMBER | |



## AwsAsg



| Name | Type | Description |
|------|------|------|
| **account** | string | AWS Account that owns this Asg |
| **arn** | string | Amazon Resource Name (ARN) of the Asg |
| **name** | string | name of the Asg (qualified by `region` and `account`) |
| **region** | string | Region (`us-east-1`, `us-west-2`, etc) |
| healthCheckType | STRING | |
| instances | ARRAY | |
| terminationPolicies | ARRAY | |
| graphEntityGroup | STRING | |
| defaultCooldown | NUMBER | |
| autoScalingGroupARN | STRING | |
| enabledMetrics | ARRAY | |
| targetGroupARNs | ARRAY | |
| availabilityZones | ARRAY | |
| maxSize | NUMBER | |
| launchTemplateName | STRING | |
| graphUpdateTs | NUMBER | |
| autoScalingGroupName | STRING | |
| graphEntityType | STRING | |
| newInstancesProtectedFromScaleIn | BOOLEAN | |
| healthCheckGracePeriod | NUMBER | |
| createdTime | NUMBER | |
| minSize | NUMBER | |
| loadBalancerNames | ARRAY | |
| serviceLinkedRoleARN | STRING | |
| tags | ARRAY | |
| launchTemplateId | STRING | |
| launchTemplateVersion | STRING | |
| suspendedProcesses | ARRAY | |
| vpczoneIdentifier | STRING | |
| desiredCapacity | NUMBER | |




## AwsAvailabilityZone



| Name | Type | Description |
|------|------|------|
| **name** | string | name of the AvailabilityZone (qualified by `region` and `account`) |
| **region** | string | Region (`us-east-1`, `us-west-2`, etc) |
| graphEntityGroup | string | always `aws` |
| graphEntityType | string | always `AwsAvailabilityZone` |
| graphUpdateTs | number | last time graph node was updated (millis since epoch) |



## AwsEc2Instance



| Name | Type | Description |
|------|------|------|
| **account** | string | AWS Account that owns this Ec2Instance |
| **arn** | string | Amazon Resource Name (ARN) of the Ec2Instance |
| **region** | string | Region (`us-east-1`, `us-west-2`, etc) |
| amiLaunchIndex | number | |
| architecture | string | |
| clientToken | string | |
| ebsOptimized | boolean | |
| elasticGpuAssociations | array | |
| enaSupport | boolean | |
| graphEntityGroup | string | always `aws` |
| graphEntityType | string | always `AwsEc2Instance` |
| graphUpdateTs | number | last time graph node was updated (millis since epoch) |
| hypervisor | string | |
| imageId | string | |
| instanceId | string | |
| instanceType | string | |
| keyName | string | |
| launchTime | number | |
| privateDnsName | string | |
| privateIpAddress | string | |
| publicDnsName | string | |
| publicIpAddress | string | |
| reservationId | string | |
| reservationOwnerId | string | |
| reservationRequesterId | string | |
| rootDeviceName | string | |
| rootDeviceType | string | |
| sourceDestCheck | boolean | |
| stateCode | number | |
| stateName | string | |
| stateTransitionReason | string | |
| subnetId | string | |
| virtualizationType | string | |
| vpcId | string | |



## AwsLambdaFunction



| Name | Type | Description |
|------|------|------|
| **account** | string | AWS Account that owns this LambdaFunction |
| **arn** | string | Amazon Resource Name (ARN) of the LambdaFunction |
| **name** | string | name of the LambdaFunction (qualified by `region` and `account`) |
| **region** | string | Region (`us-east-1`, `us-west-2`, etc) |
| codeSha256 | string | |
| codeSize | number | |
| description | string | |
| functionArn | string | |
| functionName | string | |
| graphEntityGroup | string | always `aws` |
| graphEntityType | string | always `AwsLambdaFunction` |
| graphUpdateTs | number | last time graph node was updated (millis since epoch) |
| handler | string | |
| lastModified | string | |
| memorySize | number | |
| revisionId | string | |
| role | string | |
| runtime | string | |
| timeout | number | |
| tracingConfigMode | string | |
| version | string | |
| vpcId | string | |
| vpcSubnetIds | array | |



## AwsLaunchTemplate



| Name | Type | Description |
|------|------|------|
| **account** | string | AWS Account that owns this LaunchTemplate |
| **arn** | string | Amazon Resource Name (ARN) of the LaunchTemplate |
| **name** | string | name of the LaunchTemplate (qualified by `region` and `account`) |
| **region** | string | Region (`us-east-1`, `us-west-2`, etc) |
| createTime | number | |
| createdBy | string | |
| defaultVersionNumber | number | |
| graphEntityGroup | string | always `aws` |
| graphEntityType | string | always `AwsLaunchTemplate` |
| graphUpdateTs | number | last time graph node was updated (millis since epoch) |
| latestVersionNumber | number | |
| launchTemplateId | string | |
| launchTemplateName | string | |
| tags | array | |



## AwsRegion



| Name | Type | Description |
|------|------|------|
| **region** | string | Region (`us-east-1`, `us-west-2`, etc) |
| graphEntityGroup | string | always `aws` |
| graphEntityType | string | always `AwsRegion` |
| graphUpdateTs | number | last time graph node was updated (millis since epoch) |



## AwsSecurityGroup



| Name | Type | Description |
|------|------|------|
| **account** | string | AWS Account that owns this SecurityGroup |
| **arn** | string | Amazon Resource Name (ARN) of the SecurityGroup |
| **name** | string | name of the SecurityGroup (qualified by `region` and `account`) |
| **region** | string | Region (`us-east-1`, `us-west-2`, etc) |
| description | string | |
| graphEntityGroup | string | always `aws` |
| graphEntityType | string | always `AwsSecurityGroup` |
| graphUpdateTs | number | last time graph node was updated (millis since epoch) |
| groupId | string | |
| groupName | string | |
| ipPermissionsEgress | array | |
| ownerId | string | |
| tags | array | |
| vpcId | string | |



## AwsSubnet



| Name | Type | Description |
|------|------|------|
| **account** | string | AWS Account that owns this Subnet |
| **arn** | string | Amazon Resource Name (ARN) of the Subnet |
| **region** | string | Region (`us-east-1`, `us-west-2`, etc) |
| assignIpv6AddressOnCreation | boolean | |
| availabilityZone | string | |
| availableIpAddressCount | number | |
| cidrBlock | string | |
| defaultForAz | boolean | |
| graphEntityGroup | string | always `aws` |
| graphEntityType | string | always `AwsSubnet` |
| graphUpdateTs | number | last time graph node was updated (millis since epoch) |
| ipv6CidrBlockAssociationSet | array | |
| mapPublicIpOnLaunch | boolean | |
| state | string | |
| subnetId | string | |
| vpcId | string | |



## AwsVpc



| Name | Type | Description |
|------|------|------|
| **account** | string | AWS Account that owns this Vpc |
| **arn** | string | Amazon Resource Name (ARN) of the Vpc |
| **region** | string | Region (`us-east-1`, `us-west-2`, etc) |
| cidrBlock | string | |
| default | boolean | |
| dhcpOptionsId | string | |
| graphEntityGroup | string | always `aws` |
| graphEntityType | string | always `AwsVpc` |
| graphUpdateTs | number | last time graph node was updated (millis since epoch) |
| instanceTenancy | string | |
| ipv6CidrBlockAssociationSet | array | |
| isDefault | boolean | |
| state | string | |
| vpcId | string | |




# Data Model 


## AwsAccount



| Name | Type | Description |
|------|------|------|
| **account** | string | AWS Account that owns this Account |
| graphEntityGroup | string | always `aws` |
| graphEntityType | string | always `AwsAccount` |
| graphUpdateTs | number | last time graph node was updated (millis since epoch) |



## AwsAsg



| Name | Type | Description |
|------|------|------|
| **account** | string | AWS Account that owns this Asg |
| **arn** | string | Amazon Resource Name (ARN) of the Asg |
| **name** | string | name of the Asg (qualified by `region` and `account`) |
| **region** | string | Region (`us-east-1`, `us-west-2`, etc) |
| autoScalingGroupARN | string | |
| autoScalingGroupName | string | |
| availabilityZones | array | |
| createdTime | number | |
| defaultCooldown | number | |
| desiredCapacity | number | |
| enabledMetrics | array | |
| graphEntityGroup | string | always `aws` |
| graphEntityType | string | always `AwsAsg` |
| graphUpdateTs | number | last time graph node was updated (millis since epoch) |
| healthCheckGracePeriod | number | |
| healthCheckType | string | |
| instances | array | |
| launchTemplateId | string | |
| launchTemplateName | string | |
| launchTemplateVersion | string | |
| loadBalancerNames | array | |
| maxSize | number | |
| minSize | number | |
| newInstancesProtectedFromScaleIn | boolean | |
| serviceLinkedRoleARN | string | |
| suspendedProcesses | array | |
| tags | array | |
| targetGroupARNs | array | |
| terminationPolicies | array | |
| vpczoneIdentifier | string | |



## AwsAvailabilityZone



| Name | Type | Description |
|------|------|------|
| **name** | string | name of the AvailabilityZone (qualified by `region` and `account`) |
| **region** | string | Region (`us-east-1`, `us-west-2`, etc) |
| graphEntityGroup | string | always `aws` |
| graphEntityType | string | always `AwsAvailabilityZone` |
| graphUpdateTs | number | last time graph node was updated (millis since epoch) |



## AwsEc2Instance



| Name | Type | Description |
|------|------|------|
| **account** | string | AWS Account that owns this Ec2Instance |
| **arn** | string | Amazon Resource Name (ARN) of the Ec2Instance |
| **region** | string | Region (`us-east-1`, `us-west-2`, etc) |
| amiLaunchIndex | number | |
| architecture | string | |
| clientToken | string | |
| ebsOptimized | boolean | |
| elasticGpuAssociations | array | |
| enaSupport | boolean | |
| graphEntityGroup | string | always `aws` |
| graphEntityType | string | always `AwsEc2Instance` |
| graphUpdateTs | number | last time graph node was updated (millis since epoch) |
| hypervisor | string | |
| imageId | string | |
| instanceId | string | |
| instanceType | string | |
| keyName | string | |
| launchTime | number | |
| privateDnsName | string | |
| privateIpAddress | string | |
| publicDnsName | string | |
| publicIpAddress | string | |
| reservationId | string | |
| reservationOwnerId | string | |
| reservationRequesterId | string | |
| rootDeviceName | string | |
| rootDeviceType | string | |
| sourceDestCheck | boolean | |
| stateCode | number | |
| stateName | string | |
| stateTransitionReason | string | |
| subnetId | string | |
| virtualizationType | string | |
| vpcId | string | |



## AwsLambdaFunction



| Name | Type | Description |
|------|------|------|
| **account** | string | AWS Account that owns this LambdaFunction |
| **arn** | string | Amazon Resource Name (ARN) of the LambdaFunction |
| **name** | string | name of the LambdaFunction (qualified by `region` and `account`) |
| **region** | string | Region (`us-east-1`, `us-west-2`, etc) |
| codeSha256 | string | |
| codeSize | number | |
| description | string | |
| functionArn | string | |
| functionName | string | |
| graphEntityGroup | string | always `aws` |
| graphEntityType | string | always `AwsLambdaFunction` |
| graphUpdateTs | number | last time graph node was updated (millis since epoch) |
| handler | string | |
| lastModified | string | |
| memorySize | number | |
| revisionId | string | |
| role | string | |
| runtime | string | |
| timeout | number | |
| tracingConfigMode | string | |
| version | string | |
| vpcId | string | |
| vpcSubnetIds | array | |



## AwsLaunchTemplate



| Name | Type | Description |
|------|------|------|
| **account** | string | AWS Account that owns this LaunchTemplate |
| **arn** | string | Amazon Resource Name (ARN) of the LaunchTemplate |
| **name** | string | name of the LaunchTemplate (qualified by `region` and `account`) |
| **region** | string | Region (`us-east-1`, `us-west-2`, etc) |
| createTime | number | |
| createdBy | string | |
| defaultVersionNumber | number | |
| graphEntityGroup | string | always `aws` |
| graphEntityType | string | always `AwsLaunchTemplate` |
| graphUpdateTs | number | last time graph node was updated (millis since epoch) |
| latestVersionNumber | number | |
| launchTemplateId | string | |
| launchTemplateName | string | |
| tags | array | |



## AwsRegion



| Name | Type | Description |
|------|------|------|
| **region** | string | Region (`us-east-1`, `us-west-2`, etc) |
| graphEntityGroup | string | always `aws` |
| graphEntityType | string | always `AwsRegion` |
| graphUpdateTs | number | last time graph node was updated (millis since epoch) |



## AwsSecurityGroup



| Name | Type | Description |
|------|------|------|
| **account** | string | AWS Account that owns this SecurityGroup |
| **arn** | string | Amazon Resource Name (ARN) of the SecurityGroup |
| **name** | string | name of the SecurityGroup (qualified by `region` and `account`) |
| **region** | string | Region (`us-east-1`, `us-west-2`, etc) |
| description | string | |
| graphEntityGroup | string | always `aws` |
| graphEntityType | string | always `AwsSecurityGroup` |
| graphUpdateTs | number | last time graph node was updated (millis since epoch) |
| groupId | string | |
| groupName | string | |
| ipPermissionsEgress | array | |
| ownerId | string | |
| tags | array | |
| vpcId | string | |



## AwsSubnet



| Name | Type | Description |
|------|------|------|
| **account** | string | AWS Account that owns this Subnet |
| **arn** | string | Amazon Resource Name (ARN) of the Subnet |
| **region** | string | Region (`us-east-1`, `us-west-2`, etc) |
| assignIpv6AddressOnCreation | boolean | |
| availabilityZone | string | |
| availableIpAddressCount | number | |
| cidrBlock | string | |
| defaultForAz | boolean | |
| graphEntityGroup | string | always `aws` |
| graphEntityType | string | always `AwsSubnet` |
| graphUpdateTs | number | last time graph node was updated (millis since epoch) |
| ipv6CidrBlockAssociationSet | array | |
| mapPublicIpOnLaunch | boolean | |
| state | string | |
| subnetId | string | |
| vpcId | string | |



## AwsVpc



| Name | Type | Description |
|------|------|------|
| **account** | string | AWS Account that owns this Vpc |
| **arn** | string | Amazon Resource Name (ARN) of the Vpc |
| **region** | string | Region (`us-east-1`, `us-west-2`, etc) |
| cidrBlock | string | |
| default | boolean | |
| dhcpOptionsId | string | |
| graphEntityGroup | string | always `aws` |
| graphEntityType | string | always `AwsVpc` |
| graphUpdateTs | number | last time graph node was updated (millis since epoch) |
| instanceTenancy | string | |
| ipv6CidrBlockAssociationSet | array | |
| isDefault | boolean | |
| state | string | |
| vpcId | string | |




# Other




