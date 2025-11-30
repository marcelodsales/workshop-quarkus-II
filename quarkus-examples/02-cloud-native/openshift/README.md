# Deploy dependencies on Openshift

## Using provided yaml
```shell
oc apply -f postgres.yaml
```

## Using Templates

### Show templates for postgres
```shell
oc get templates -n openshift | grep postgresql
```

### Show environment variables for the template
```shell
oc process --parameters -n openshift postgresql-persistent
```

### Get detailed information about the template
```shell
oc describe template postgresql-persistent -n openshift
```

### Deploy Template
```shell
oc new-app --name=postgresql postgresql-persistent \  
  -p POSTGRESQL_DATABASE=banking_db \
  -p VOLUME_CAPACITY=1Gi \
  -p POSTGRESQL_VERSION=latest
```