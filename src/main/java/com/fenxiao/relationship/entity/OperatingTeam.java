package com.fenxiao.relationship.entity;

import com.fenxiao.common.entity.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name="operating_team")
public class OperatingTeam extends BaseEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="team_code",nullable=false,unique=true) private String teamCode;
    @Column(name="team_name",nullable=false) private String teamName;
    @Column(name="country_code",nullable=false) private String countryCode;
    @Column(name="leader_user_id") private Long leaderUserId;
    @Column(name="team_status",nullable=false) private String teamStatus;
    protected OperatingTeam(){}
    public static OperatingTeam create(String code,String name,String country,Long leader){var t=new OperatingTeam();t.teamCode=code;t.teamName=name;t.countryCode=country;t.leaderUserId=leader;t.teamStatus="ACTIVE";return t;}
    public Long getId(){return id;} public String getTeamCode(){return teamCode;} public String getCountryCode(){return countryCode;} public Long getLeaderUserId(){return leaderUserId;}
}
