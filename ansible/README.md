# Ansible Deployment

Copy the `ansible` folder into the root of your GitHub project repository. Use the steps below to update the configuration for your own project.

## 1. Update `inventory.ini`

Open `inventory.ini` in VS Code and replace the example IP with your **Tomcat EC2 instance's private IP**.

```ini
[calculator]
calculator ansible_host=<YOUR_EC2_PRIVATE_IP> ansible_user=ec2-user
```

Replace `<YOUR_EC2_PRIVATE_IP>` with your target EC2 private IP. Keep `ansible_user` as `ec2-user` if that is the SSH user configured on your instance.

## 2. Update `deploy-calculator.yml`

Open `deploy-calculator.yml` and check these values. Update them to match your own Nexus repository and Maven project:

- `nexus_base_url` — your Nexus server URL
- `nexus_repository` — the Maven repository where your artifact is published
- `nexus_group_path` — the Maven group path, such as `com/example`
- `nexus_artifact_id` — your project's Maven `artifactId`
- `tomcat_webapps` — the Tomcat `webapps` directory on your target EC2, if different
- `app_context` — the application name used in the URL

The Nexus group path and artifact ID must match your Maven project and the artifact published to Nexus.

## 3. Copy the encrypted Vault content from EC2 into VS Code

The existing `nexus-vault.yml` on the Jenkins/Ansible EC2 contains encrypted credentials. Copy the **encrypted content only** into the `ansible/nexus-vault.yml` file in your GitHub project.

1. Connect to the EC2 instance where the existing Ansible files are stored.
2. Find the existing Vault file. For example, if your file is in `/home/ec2-user/ansible-calculator-lab`, run:

   ```bash
   cat /home/ec2-user/ansible-calculator-lab/nexus-vault.yml
   ```

3. Copy the complete output, starting with the line:

   ```text
   $ANSIBLE_VAULT;1.1;AES256
   ```

   Include **all** the encrypted lines through the end of the file. Do not copy terminal prompts or other command output.

4. In VS Code, open your project's `ansible/nexus-vault.yml`.
5. Replace its contents with the encrypted text you copied. Save the file.
6. Confirm that the first line begins with `$ANSIBLE_VAULT;`. The file must contain encrypted data, not a plain-text username or password.
7. Commit and push the file to GitHub.

**Important:** The encrypted file must be decrypted using the matching Ansible Vault password. Configure that password in Jenkins as the Secret text credential `ansible-vault-password`. If the password does not match, the deployment stage will fail.

Do not paste or commit the plain-text Nexus username/password into GitHub. Treat the encrypted file and its Vault password as sensitive. If the existing Vault password has been exposed or is no longer known, create a new Vault file with a new password instead of reusing the old encrypted content.

## 4. Check `install-java.yml` and `install-tomcat.yml`

Review these files and confirm that the Java package, Tomcat installation directory, service name, and service account match your target EC2 setup. If the files already match your lab environment, you may not need to change them.

## 5. Configure Jenkins credentials

Make sure these credentials exist in Jenkins and that the pipeline uses the same credential IDs:

- `ansible-target-ssh-student` — SSH private key for connecting to the target EC2
- `ansible-vault-password` — Secret text containing the password for the encrypted Vault file
- Your existing Nexus publishing credential — used by the pipeline to publish the Maven artifact

The Jenkins pipeline should run the Ansible playbooks from the `ansible` folder in the checked-out GitHub workspace.

## 6. Final checklist

- [ ] `inventory.ini` has the correct target EC2 private IP and SSH user.
- [ ] `deploy-calculator.yml` matches your Nexus repository and Maven artifact details.
- [ ] `nexus-vault.yml` contains the complete encrypted Vault content.
- [ ] Jenkins has the SSH key and matching Vault password credentials.
- [ ] Java and Tomcat settings match the target EC2 instance.
- [ ] Changes are committed and pushed to GitHub.

Once these checks are complete, run the Jenkins pipeline and review the console output if a stage fails.
