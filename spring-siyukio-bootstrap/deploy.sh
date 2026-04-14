#!/bin/bash

set -e

# Enable BuildKit for caching
export DOCKER_BUILDKIT=1

IMAGE_NAME="siyukio-bootstrap"
# Use pom.xml version + git commit hash as image tag
POM_VERSION=$(grep "<version>" pom.xml | head -1 | sed 's/.*<version>\(.*\)<\/version>.*/\1/')
COMMIT_HASH=$(git rev-parse --short HEAD)
IMAGE_TAG="${POM_VERSION}-${COMMIT_HASH}"
REGISTRY="localhost:5000"
FULL_IMAGE_NAME="${REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}"
ENABLE_INGRESS=${ENABLE_INGRESS:-true}

echo "========================================="
echo "Building Docker image..."
echo "========================================="
docker build --progress=plain -t ${IMAGE_NAME}:${IMAGE_TAG} .

echo "========================================="
echo "Loading image to Minikube..."
echo "========================================="
minikube image load ${IMAGE_NAME}:${IMAGE_TAG}

echo "========================================="
echo "Deploying to Kubernetes..."
echo "========================================="
# Replace IMAGE_TAG placeholder in deployment.yaml with actual version
sed "s/IMAGE_TAG/${IMAGE_TAG}/g" k8s/deployment.yaml | kubectl apply -f -

if [ "$ENABLE_INGRESS" = "true" ]; then
  echo "========================================="
  echo "Enabling Nginx Ingress Controller..."
  echo "========================================="
  minikube addons enable ingress

  echo "========================================="
  echo "Deploying Ingress..."
  echo "========================================="
  kubectl apply -f k8s/ingress.yaml

  echo "========================================="
  echo "Adding host to /etc/hosts..."
  echo "========================================="
  if ! grep -q "siyukio.local" /etc/hosts; then
    echo "127.0.0.1 siyukio.local" | sudo tee -a /etc/hosts
  else
    echo "Host siyukio.local already exists in /etc/hosts, updating to 127.0.0.1"
    sudo sed -i '' '/siyukio.local/s/^.*$/127.0.0.1 siyukio.local/' /etc/hosts
  fi
fi

echo "========================================="
echo "Waiting for deployment to be ready..."
echo "========================================="
kubectl rollout status deployment/siyukio-bootstrap --timeout=120s

echo "========================================="
echo "Deployment completed!"
echo "========================================="
echo "Access your application:"
if [ "$ENABLE_INGRESS" = "true" ]; then
  echo "  - Ingress URL: http://siyukio.local"
else
  echo "  - Service: kubectl port-forward svc/siyukio-bootstrap 8080:80"
fi
echo ""
echo "Check pods:"
echo "  kubectl get pods -l app=siyukio-bootstrap"
echo ""
echo "Check logs:"
echo "  kubectl logs -l app=siyukio-bootstrap -f"
echo ""
echo "Delete deployment:"
echo "  kubectl delete -f k8s/deployment.yaml"
if [ "$ENABLE_INGRESS" = "true" ]; then
  echo "  kubectl delete -f k8s/ingress.yaml"
  echo "  sudo sed -i '' '/siyukio.local/d' /etc/hosts"
fi
