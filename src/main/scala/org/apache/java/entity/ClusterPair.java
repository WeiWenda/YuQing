package org.apache.java.entity;

/**
 * Created by david on 12/23/15.
 */

import java.io.Serializable;

/*******************************************************************************
 * Copyright 2013 Lars Behnke
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

public class ClusterPair implements Comparable<ClusterPair>,Serializable {

    private Cluster lCluster;
    private Cluster rCluster;
    private Double linkageDistance;

    public ClusterPair() {

    }
    public ClusterPair(Cluster lCluster,Cluster rCluster,Double linkageDistance) {
        this.lCluster=lCluster;
        this.rCluster=rCluster;
        this.linkageDistance=linkageDistance;
    }

    public Cluster getlCluster() {
        return lCluster;
    }

    public void setlCluster(Cluster lCluster) {
        this.lCluster = lCluster;
    }

    public Cluster getrCluster() {
        return rCluster;
    }

    public void setrCluster(Cluster rCluster) {
        this.rCluster = rCluster;
    }

    public Double getLinkageDistance() {
        return linkageDistance;
    }

    public void setLinkageDistance(Double distance) {
        this.linkageDistance = distance;
    }

    @Override
    public int compareTo(ClusterPair o) {
        int result;
        if (o == null || o.getLinkageDistance() == null) {
            result = -1;
        } else if (getLinkageDistance() == null) {
            result = 1;
        } else {
            result = getLinkageDistance().compareTo(o.getLinkageDistance());
        }

        return result;
    }

    public Cluster agglomerate(String name) {
        if (name == null) {
            StringBuilder sb = new StringBuilder();
            if (lCluster != null) {
                sb.append(lCluster.getName());
            }
            if (rCluster != null) {
                if (sb.length() > 0) {
                    sb.append("&");
                }
                sb.append(rCluster.getName());
            }
            name = sb.toString();
        }
        Cluster cluster = new Cluster(name);
        cluster.setDistance(getLinkageDistance());
        cluster.addChild(lCluster);
        cluster.addChild(rCluster);
        cluster.setCountOfWords(lCluster.getCountOfWords()+rCluster.getCountOfWords());
        lCluster.setParent(cluster);
        rCluster.setParent(cluster);
        return cluster;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (lCluster != null) {
            sb.append(lCluster.getName());
        }
        if (rCluster != null) {
            if (sb.length() > 0) {
                sb.append(" + ");
            }
            sb.append(rCluster.getName());
        }
        sb.append(" : ").append(linkageDistance);
        return sb.toString();
    }

    @Override
    public int hashCode(){
        return lCluster.hashCode() + rCluster.hashCode();
    }

    @Override
    public boolean equals(Object pair) {
        ClusterPair clusterPair = (ClusterPair)pair;
        return (lCluster.equals(clusterPair.lCluster) && rCluster.equals(clusterPair.rCluster))
                || (lCluster.equals(clusterPair.rCluster) && rCluster.equals(clusterPair.lCluster));
    }
}
