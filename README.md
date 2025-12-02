# Workshop Quarkus II

## Running the Ansible playbooks

From the project root (`/home/masales/RedHat/Workshops/Quarkus_II`), use:

- **Quarkus 01 – developer joy (English)**  
  `ansible-playbook playbook-01.yaml`

- **Quarkus 01 – developer joy (Spanish)**  
  `ansible-playbook playbook-01.yaml -e user_language=es`

- **Quarkus 02 – cloud native (English)**  
  `ansible-playbook playbook-02.yaml`

- **Quarkus 02 – cloud native (Spanish)**  
  `ansible-playbook playbook-02.yaml -e user_language=es`

Requirements:
- Ansible installed and available in `PATH`.
- Podman and podman-compose available for local database/containers.
- For OpenShift/Knative steps: `oc` logged into a cluster with the required operators installed.

