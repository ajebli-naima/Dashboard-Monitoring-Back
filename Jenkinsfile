node('') {

stage 'buildInDevelopment'

openshiftBuild(namespace: 'cicd', buildConfig: 'dash-monitoring-back', showBuildLogs: 'true')

stage 'deployInDevelopment'

openshiftDeploy(namespace: 'cicd', deploymentConfig: 'dash-monitoring-back')

openshiftScale(namespace: 'cicd', deploymentConfig: 'dash-monitoring-back',replicaCount: '2')

}
