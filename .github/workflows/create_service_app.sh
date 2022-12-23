set -ex

az extension add -n spring
az account set -s $subscription
az group create -l $location -n $rg_name
az spring create -g $rg_name -n $service_name
az spring app create -n $app_name -g $rg_name -s $service_name