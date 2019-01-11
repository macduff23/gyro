package beam.aws.iam;

import beam.aws.AwsResource;
import beam.core.BeamException;
import beam.core.BeamResource;
import beam.core.diff.ResourceDiffProperty;
import beam.core.diff.ResourceName;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.AttachedPolicy;
import software.amazon.awssdk.services.iam.model.ListAttachedRolePoliciesResponse;
import software.amazon.awssdk.services.iam.model.Role;

import java.net.URLDecoder;

import java.nio.file.Files;
import java.nio.file.Paths;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Creates a Role Resource with the specified options.
 *
 * Example
 * -------
 *
 * .. code-block:: beam
 *
 *     aws::role-resource example-role
 *         description: testing the role functionality
 *         assume-role-policy-document-file: role_example
 *         role-name: rta-test-role
 *
 *     end
 */

@ResourceName("iam-role")
public class IamRoleResource extends AwsResource {

    private String roleName;
    private String description;
    private String assumeRolePolicyContents;
    private String assumeRolePolicyDocumentFile;
    private List<String> policyArns;

    @ResourceDiffProperty(updatable = true)
    public String getAssumeRolePolicyContents() {

        if(assumeRolePolicyContents != null){
            return assumeRolePolicyContents;
        }
        else {
            if(getAssumeRolePolicyDocumentFile() != null) {
                try {
                    String encode = new String(Files.readAllBytes(Paths.get(getAssumeRolePolicyDocumentFile())), "UTF-8");
                    return formatPolicy(encode);
                } catch (Exception err) {
                    throw new BeamException(err.getMessage());
                }
            } else {
                return null;
            }
        }
    }

    public void setAssumeRolePolicyContents(String assumeRolePolicyContents) {
        this.assumeRolePolicyContents = assumeRolePolicyContents;
    }

    @ResourceDiffProperty(updatable = true)
    public String getAssumeRolePolicyDocumentFile() {
        return this.assumeRolePolicyDocumentFile;
    }

    public void setAssumeRolePolicyDocumentFile(String assumeRolePolicyDocumentFile) {
        this.assumeRolePolicyDocumentFile = assumeRolePolicyDocumentFile;
    }

    @ResourceDiffProperty(updatable = true)
    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @ResourceDiffProperty(updatable = true, nullable = true)
    public List<String> getPolicyArns() {
        if (this.policyArns == null) {
            this.policyArns = new ArrayList<>();
        }

        return this.policyArns;
    }

    public void setPolicyArns(List<String> policyArns) {
        this.policyArns = policyArns;
    }

    public String getRoleName() {
        return this.roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    @Override
    public boolean refresh() {
        IamClient client = IamClient.builder()
                .region(Region.AWS_GLOBAL)
                .build();

        Role response = client.getRole(r -> r.roleName(getRoleName())).role();
        if (response != null) {
            setRoleName(response.roleName());
            setDescription(response.description());
            String encode = URLDecoder.decode(response.assumeRolePolicyDocument());
            setAssumeRolePolicyContents(formatPolicy(encode));

            getPolicyArns().clear();
            ListAttachedRolePoliciesResponse policyResponse = client.listAttachedRolePolicies(r -> r.roleName(getRoleName()));
            for (AttachedPolicy attachedPolicy: policyResponse.attachedPolicies()) {
                getPolicyArns().add(attachedPolicy.policyArn());
            }

            return true;
        }

        return false;
    }

    @Override
    public void create() {
        IamClient client = IamClient.builder()
                .region(Region.AWS_GLOBAL)
                .build();

        client.createRole(r -> r.assumeRolePolicyDocument(getAssumeRolePolicyContents())
                                .description(getDescription())
                                .roleName(getRoleName()));

        for (String policyArn: getPolicyArns()) {
            client.attachRolePolicy(r -> r.roleName(getRoleName())
                    .policyArn(policyArn));
        }
    }

    @Override
    public void update(BeamResource current, Set<String> changedProperties) {
        IamClient client = IamClient.builder()
                .region(Region.AWS_GLOBAL)
                .build();

        IamRoleResource currentResource = (IamRoleResource) current;

        client.updateAssumeRolePolicy(r -> r.policyDocument(formatPolicy(getAssumeRolePolicyContents()))
                                            .roleName(getRoleName()));

        client.updateRole(r -> r.description(getDescription())
                                .roleName(getRoleName()));

        List<String> additions = new ArrayList<>(getPolicyArns());
        additions.removeAll(currentResource.getPolicyArns());

        List<String> subtractions = new ArrayList<>(currentResource.getPolicyArns());
        subtractions.removeAll(getPolicyArns());

        for (String addPolicyArn : additions) {
            client.attachRolePolicy(r -> r.policyArn(addPolicyArn)
                    .roleName(getRoleName()));
        }

        for (String deletePolicyArn : subtractions) {
            client.detachRolePolicy(r -> r.policyArn(deletePolicyArn)
                    .roleName(getRoleName()));
        }

    }

    @Override
    public void delete() {
        IamClient client = IamClient.builder()
                .region(Region.AWS_GLOBAL)
                .build();

        ListAttachedRolePoliciesResponse response = client.listAttachedRolePolicies(r -> r.roleName(getRoleName()));
        for(AttachedPolicy policies : response.attachedPolicies()){
            client.detachRolePolicy(r -> r.policyArn(policies.policyArn())
                                        .roleName(getRoleName()));
        }

        client.deleteRole(r -> r.roleName(getRoleName()));

    }

    @Override
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();

        if (getRoleName() != null) {
            sb.append("role " + getRoleName());

        } else {
            sb.append("role ");
        }

        return sb.toString();
    }

    public String formatPolicy(String policy) {
        return policy != null ? policy.replaceAll(System.lineSeparator(), " ").replaceAll("\t", " ").trim().replaceAll(" ", "") : policy;
    }
}