node('') {

stage 'buildInDevelopment'

openshiftBuild(namespace: 'my-project', buildConfig: 'dash-monitoring-back', showBuildLogs: 'true')

stage 'deployInDevelopment'

openshiftDeploy(namespace: 'my-project', deploymentConfig: 'dash-monitoring-back')

openshiftScale(namespace: 'my-project', deploymentConfig: 'dash-monitoring-back',replicaCount: '3')

}
