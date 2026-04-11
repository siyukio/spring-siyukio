# Start minikube

```
minikube start
```

# Deploy

```
./deploy.sh
```

# Check status

```
kubectl get pods -l app=siyukio-bootstrap
kubectl logs -l app=siyukio-bootstrap -f
```

# Check ingress controller status

```
kubectl get svc -n ingress-nginx
```

Expected output:

```
NAME                                 TYPE           CLUSTER-IP       EXTERNAL-IP   PORT(S)                      AGE
ingress-nginx-controller-lb          LoadBalancer   10.108.9.168     <pending>     80:31994/TCP,443:30637/TCP   2m55s
```

Note: Wait for EXTERNAL-IP to change from `<pending>` to `127.0.0.1`. Make sure minikube tunnel is running: `sudo minikube tunnel`

# Access application

During deployment, the domain `siyukio.local` is automatically bound to `127.0.0.1`. You can access the application via:

```
http://siyukio.local
```
