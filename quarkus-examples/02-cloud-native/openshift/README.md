# Openshift

## Deploy dependencies on Openshift

### Using provided yaml
```shell
oc apply -f postgres.yaml
```

### Using Templates

#### Show templates for postgres
```shell
oc get templates -n openshift | grep postgresql
```

#### Show environment variables for the template
```shell
oc process --parameters -n openshift postgresql-persistent
```

#### Get detailed information about the template
```shell
oc describe template postgresql-persistent -n openshift
```

#### Deploy Template
```shell
oc new-app --name=postgresql postgresql-persistent \  
  -p POSTGRESQL_DATABASE=banking_db \
  -p VOLUME_CAPACITY=1Gi \
  -p POSTGRESQL_VERSION=latest
```

---

## Serverless

### Create serverles resource
```shell
oc apply -f banking-serverless.yaml

kn service create banking-serverless \
  --image image-registry.openshift-image-registry.svc:5000/bbva-quarkus-ii/banking-demo:1.0.0-SNAPSHOT \
  --port 8080 \
  --env-from secret:postgresql-secret \
  --env-from config-map:banking-config

```

### Monitoring pods
```shell
watch oc get pods
```

### Get Pod URL
```shell
oc get ksvc banking-serverless
```

### Watch Scale Pods
```shell
while true; do curl http://url.../api/banking; echo; done
```