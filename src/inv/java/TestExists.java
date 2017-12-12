import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.scm4j.releaser.WorkflowTestBase;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.BasicAuthenticationManager;
import org.tmatesoft.svn.core.auth.SVNAuthentication;
import org.tmatesoft.svn.core.auth.SVNPasswordAuthentication;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;

public class TestExists extends WorkflowTestBase {

	@Test
	public void testUsingSVNKit() throws Exception {
		
		// https://github.com/ProjectKaiser/tf-server-toolkit
			
//			
//		vcs.createBranch(null, "test", "test branch created");
//		vcs.setFileContent("test", "version", "2.59.0", "version file added");
//		
//		long start = System.currentTimeMillis();
//		System.out.println(vcs.getFileContent("test", "version", null));
//		System.out.println(System.currentTimeMillis() - start + " ms");
//		
//		start = System.currentTimeMillis();
//		System.out.println(vcs.getBranches("").contains("test"));
//		System.out.println(System.currentTimeMillis() - start + " ms");
		
		SVNURL trunkSVNUrl = SVNURL.parseURIEncoded("https://github.com/scm4j/scm4j-releaser");
		SVNRepository repository = SVNRepositoryFactory.create(trunkSVNUrl);
		SVNAuthentication userPassAuth = SVNPasswordAuthentication.newInstance("gmp", "".toCharArray(),  
				true, trunkSVNUrl, false);
		SVNAuthentication[] auth = new SVNAuthentication[] {userPassAuth};
		BasicAuthenticationManager authManager = new BasicAuthenticationManager(auth);
		repository.setAuthenticationManager(authManager);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		System.out.println("working...");
		
		listEntries(repository, "branches/");
		
		long start;

		start = System.currentTimeMillis();
		repository.getFile("trunk/version", -1L, new SVNProperties(), baos);
		System.out.println("getFile() " + (System.currentTimeMillis() - start));

		
		start = System.currentTimeMillis();
		List<String> entries = listEntries(repository, "branches/");
		System.out.println("getDir() " + (System.currentTimeMillis() - start));
		
		
		start = System.currentTimeMillis();
		repository.checkPath("trunk/", -1);
		System.out.println("getPath() " + (System.currentTimeMillis() - start));
		
		//   System.out.println(baos.toString(StandardCharsets.UTF_8.name()));
		
		
		
	}
	
//	@Test
//	public void testUsingVCSAPI() {
//		File repoDir = new File(TestEnvironment.TEST_REMOTE_REPO_DIR, "unTill-");
//		IVCSWorkspace ws = new VCSWorkspace(TestEnvironment.TEST_VCS_WORKSPACES_DIR);
//		IVCSRepositoryWorkspace repoWS;
//		//IVCS vcs = new SVNVCS(repoWS, )
//	}
	
	protected List<String> listEntries(SVNRepository repository, String path) throws Exception {
		List<String> res = new ArrayList<>();
		if (path == null) {
			return res;
		}
		path = path.trim();
		String lastFolder;
		String folderPrefix;
		int lastSlashIndex = path.lastIndexOf("/");
		lastFolder = lastSlashIndex > 0 ? path.substring(0, lastSlashIndex) : path;
		folderPrefix =  lastSlashIndex > 0 ? path.substring(lastSlashIndex + 1) : "";
//		if (repository.checkPath(lastFolder , -1) == SVNNodeKind.NONE) {
//			return res;
//		}

		@SuppressWarnings("unchecked")
		Collection<SVNDirEntry> entries = repository.getDir(lastFolder, -1 , null , (Collection<SVNDirEntry>) null);
		List<SVNDirEntry> entriesList = new ArrayList<>(entries);
		Collections.sort(entriesList, new Comparator<SVNDirEntry>() {
			@Override
			public int compare(SVNDirEntry o1, SVNDirEntry o2) {
				if (o1.getRevision() < o2.getRevision()) {
					return -1;
				}
				if (o1.getRevision() > o2.getRevision()) {
					return 1;
				}
				return 0;
			}
		});
		for (SVNDirEntry entry : entriesList) {
			if (entry.getKind() == SVNNodeKind.DIR && entry.getName().startsWith(folderPrefix)) {
				String branchName = (path.isEmpty() ? "" : StringUtils.appendIfMissing(lastFolder, "/")) + entry.getName();
				res.add(branchName);
			}
		}
		
		return res;
	}

}
