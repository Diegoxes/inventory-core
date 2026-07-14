package com.smarthome.service;

import com.smarthome.entity.Organization;
import com.smarthome.entity.OrganizationMember;
import com.smarthome.entity.User;
import com.smarthome.repository.OrganizationMemberRepository;
import com.smarthome.repository.OrganizationRepository;
import com.smarthome.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrgMemberAttachService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository memberRepository;

    @Transactional
    public void attachToOrganization(String userId, String organizationId, OrganizationMember.OrgRole orgRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        attachToOrganization(user, organizationId, orgRole);
    }

    @Transactional
    public void attachToOrganization(User user, String organizationId, OrganizationMember.OrgRole orgRole) {
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new RuntimeException("Organización no encontrada"));

        if (memberRepository.findByUserId(user.getId()).isPresent()) {
            throw new RuntimeException("El usuario ya pertenece a una organización");
        }

        long count = memberRepository.countByOrganizationId(org.getId());
        if (count >= org.getMaxMembers()) {
            throw new RuntimeException("Límite de miembros alcanzado en la organización (" + org.getMaxMembers() + ")");
        }

        assertSingleManager(org.getId(), orgRole, null);

        memberRepository.save(OrganizationMember.builder()
                .organization(org)
                .user(user)
                .orgRole(orgRole)
                .build());
    }

    private void assertSingleManager(String orgId, OrganizationMember.OrgRole orgRole, String excludeMemberId) {
        if (orgRole != OrganizationMember.OrgRole.MANAGER) {
            return;
        }
        long managers = memberRepository.countByOrganizationIdAndOrgRole(orgId, OrganizationMember.OrgRole.MANAGER);
        if (excludeMemberId != null && managers > 0) {
            OrganizationMember existing = memberRepository.findById(excludeMemberId).orElse(null);
            if (existing != null && existing.getOrgRole() == OrganizationMember.OrgRole.MANAGER) {
                return;
            }
        }
        if (managers >= 1) {
            throw new RuntimeException("Solo puede haber un MANAGER por organización. Elige MEMBER o VIEWER.");
        }
    }
}
