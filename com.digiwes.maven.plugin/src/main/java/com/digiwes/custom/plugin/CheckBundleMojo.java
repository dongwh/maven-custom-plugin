package com.digiwes.custom.plugin;

import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactCollector;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.execution.RuntimeInformation;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilder;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilderException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Created by dongwh on 2016-3-30.
 *
 * Goal which touches a timestamp file.
 *
 * @goal bundle
 *
 * @phase process-sources
 *
 * @requiresDependencyResolution test
 */
public class CheckBundleMojo extends AbstractMojo {
    private static final String[] INCLUDES_DEFAULT={"java","xml"};
    /**
     * The Maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * The artifact repository to use.
     *
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    private ArtifactRepository localRepository;

    /**
     * Runtime Information used to check the Maven version
     * @since 2.0
     * @component role="org.apache.maven.execution.RuntimeInformation"
     */
    private RuntimeInformation rti;

    /**
     * The dependency tree builder to use.
     *
     * @component
     * @required
     * @readonly
     */
    private DependencyTreeBuilder dependencyTreeBuilder;
    /**
     * The computed dependency tree root node of the Maven project.
     */
    private DependencyNode rootNode;
    /**
     * The artifact factory to use.
     *
     * @component
     * @required
     * @readonly
     */
    private ArtifactFactory artifactFactory;

    /**
     * The artifact metadata source to use.
     *
     * @component
     * @required
     * @readonly
     */
    private ArtifactMetadataSource artifactMetadataSource;
    /**
     * The artifact collector to use.
     *
     * @component
     * @required
     * @readonly
     */
    private ArtifactCollector artifactCollector;
    /**
     * The scope to filter by when resolving the dependency tree, or <code>null</code> to include dependencies from
     * all scopes. Note that this feature does not currently work due to MNG-3236.
     *
     * @since 2.0-alpha-5
     * @see <a href="http://jira.codehaus.org/browse/MNG-3236">MNG-3236</a>
     *
     * @parameter expression="${scope}"
     */
    private String scope;

    /** jar detail info */
    private Map jarInfoMap = new HashMap();

    private final String GROUP_ID="groupId";
    private final String ARTIFACT_ID="artifactId";
    private final String BUNDLE_TYPE="bundleType";
    private final String VERSION="version";

    /**
     * @parameter
     */
    private String[] includes;

    /**
     * @parameter
     */
    private String something;

    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("custom maven plugin start---------------------------------------------------");
        getLog().info("localRepository.getUrl()" + localRepository.getUrl());
        getLog().info("localRepository.getBasedir()=" + localRepository.getBasedir());

        getLog().info("includes==============="+includes);
        if(null==includes || includes.length==0){
            includes = INCLUDES_DEFAULT;
        }
        getLog().info("includes==============="+includes);
        for(String include : includes){
            getLog().info("include==============="+include);
        }

        ArtifactVersion detectedMavenVersion = rti.getApplicationVersion();

        ArtifactFilter artifactFilter = createResolvingArtifactFilter();
        try {
            rootNode = dependencyTreeBuilder.buildDependencyTree( project, localRepository, artifactFactory,
                            artifactMetadataSource, artifactFilter, artifactCollector );
            String rootNodeString = rootNode.toString();
            List<String> bundleInfoList = readTreeInfo(rootNodeString);
            if(null!=bundleInfoList && bundleInfoList.size()>0){
                for(String bundleInfo : bundleInfoList){
                    getLog().info("    "+bundleInfo);
                }
            }
            /*String bundleFullName = "D:\\Tools\\WorkingTools\\Maven\\mavenRepositories\\commons-logging\\commons-logging\\1.2\\commons-logging-1.2.jar";
            readJarInfo(bundleFullName);*/
        }catch (DependencyTreeBuilderException exception){
            throw new MojoExecutionException( "Cannot build project dependency tree", exception );
        }

    }
    /**
     * Gets the artifact filter to use when resolving the dependency tree.
     *
     * @return the artifact filter
     */
    private ArtifactFilter createResolvingArtifactFilter()
    {
        ArtifactFilter filter;
        // filter scope
        if ( scope != null )
        {
            getLog().debug( "+ Resolving dependency tree for scope '" + scope + "'" );
            filter = new ScopeArtifactFilter( scope );
        }
        else
        {
            filter = null;
        }
        return filter;
    }

    private boolean process(InputStream input) throws IOException {
        boolean bundleState = false;
        InputStreamReader isr = new InputStreamReader(input);
        BufferedReader reader = new BufferedReader(isr);
        String line;
        while ((line = reader.readLine()) != null) {
            //getLog().info("line=================================" + line);
            if(line.startsWith("Bundle-SymbolicName")){
                bundleState =true;
                break;
            }
        }
        reader.close();
        return bundleState;
    }
    /**
     * read jar info
     */
    private boolean readJarInfo(String bundleFullName){
        boolean bundleStatus = false;
        try {
            //getLog().info("read jar info start");
            //getLog().info("bundleFullName==========="+bundleFullName);
            JarFile jarFile = new JarFile(bundleFullName);
            JarEntry entry = jarFile.getJarEntry("META-INF/MANIFEST.MF");
            /*BufferedReader reader = new BufferedReader(new InputStreamReader(jarFile.getInputStream(entry)));
            String strLine = reader.readLine();
            getLog().info("jar strLine====" + strLine);*/

            InputStream input = jarFile.getInputStream(entry);
            bundleStatus = process(input);
            jarFile.close();
        }catch (Exception e){
            getLog().info("read jar info error");
            new IOException("read jar info error");
        }
        return bundleStatus;
    }

    /**
     * read tree info
     */
    private List<String> readTreeInfo(String nodeInfo){
        List<String> bundleInfoList = new ArrayList<String>();
        String bundleFullPath;
        String[] jarDetails = nodeInfo.split("\n");
        if(null!=jarDetails && jarDetails.length>0){
            for(String jarDetail : jarDetails){
                jarDetail = jarDetail.trim();  //每个bundle的信息
                getLog().info("jarDetailStr==================" + jarDetail);
                //如果jar是以 "(" 开始的 不去读取信息
                if(jarDetail.startsWith("(")){
                    continue;
                }
                Map<String, String> jarDetailInfo = parsingJarDetail(jarDetail);
                String groupId = jarDetailInfo.get(GROUP_ID).replaceAll("\\.", "/");
                String artifactId = jarDetailInfo.get(ARTIFACT_ID);
                String version = jarDetailInfo.get(VERSION);
                //String bundleType = jarDetailInfo.get(BUNDLE_TYPE);
                //bundleFullPath = localRepository.getUrl() + groupId + "/" + artifactId + "/" + version + "/" + artifactId+"-"+version+"."+bundleType;
                bundleFullPath = localRepository.getBasedir() + "\\"+ groupId + "\\" + artifactId + "\\" + version + "\\" + artifactId+"-"+version+".jar";
                //读取jar包中的详细信息（.mf文件）
                boolean bundleStatus = readJarInfo(bundleFullPath);
                jarDetail += ":"+bundleStatus;
                bundleInfoList.add(jarDetail);
            }
        }
        return bundleInfoList;
    }

    /**
     * parsing Jar Detail and obtain groupId and artifactId
     */
    private Map parsingJarDetail(String jarInfo){
        String[] res = jarInfo.split(":");
        if(null!=res && res.length>=3){
            jarInfoMap.put(GROUP_ID,res[0]);
            jarInfoMap.put(ARTIFACT_ID,res[1]);
            jarInfoMap.put(BUNDLE_TYPE,res[2]);
            jarInfoMap.put(VERSION,res[3]);
        }
        return jarInfoMap;
    }

}
