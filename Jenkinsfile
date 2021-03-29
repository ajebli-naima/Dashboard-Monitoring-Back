node('') {

stage 'buildInDevelopment'

openshiftBuild(namespace: 'cicd', buildConfig: 'dashboard-back', showBuildLogs: 'true')

stage 'deployInDevelopment'

openshiftDeploy(namespace: 'cicd', deploymentConfig: 'dashboard-back')

openshiftScale(namespace: 'cicd', deploymentConfig: 'dashboard-back',replicaCount: '2')

}
