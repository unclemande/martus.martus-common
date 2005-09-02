/*

The Martus(tm) free, social justice documentation and
monitoring software. Copyright (C) 2001-2004, Beneficent
Technology, Inc. (Benetech).

Martus is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either
version 2 of the License, or (at your option) any later
version with the additions and exceptions described in the
accompanying Martus license file entitled "license.txt".

It is distributed WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, including warranties of fitness of purpose or
merchantability.  See the accompanying Martus License and
GPL license for more details on the required license terms
for this software.

You should have received a copy of the GNU General Public
License along with this program; if not, write to the Free
Software Foundation, Inc., 59 Temple Place - Suite 330,
Boston, MA 02111-1307, USA.

*/

package org.martus.common.bulletinstore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.martus.common.LoggerInterface;
import org.martus.common.MartusUtilities;
import org.martus.common.MartusUtilities.FileVerificationException;
import org.martus.common.bulletin.Bulletin;
import org.martus.common.bulletin.BulletinZipUtilities;
import org.martus.common.bulletin.PendingAttachmentList;
import org.martus.common.crypto.MartusCrypto;
import org.martus.common.crypto.StreamEncryptor;
import org.martus.common.crypto.MartusCrypto.CryptoException;
import org.martus.common.crypto.MartusCrypto.DecryptionException;
import org.martus.common.crypto.MartusCrypto.NoKeyPairException;
import org.martus.common.database.Database;
import org.martus.common.database.DatabaseKey;
import org.martus.common.database.ReadableDatabase;
import org.martus.common.database.Database.RecordHiddenException;
import org.martus.common.database.FileDatabase.MissingAccountMapException;
import org.martus.common.database.FileDatabase.MissingAccountMapSignatureException;
import org.martus.common.packet.BulletinHeaderPacket;
import org.martus.common.packet.BulletinHistory;
import org.martus.common.packet.FieldDataPacket;
import org.martus.common.packet.Packet;
import org.martus.common.packet.UniversalId;
import org.martus.common.packet.Packet.InvalidPacketException;
import org.martus.common.packet.Packet.SignatureVerificationException;
import org.martus.common.packet.Packet.WrongAccountException;
import org.martus.common.packet.Packet.WrongPacketTypeException;
import org.martus.util.StreamCopier;
import org.martus.util.StreamFilter;
import org.martus.util.Base64.InvalidBase64Exception;
import org.martus.util.inputstreamwithseek.InputStreamWithSeek;
import org.martus.util.inputstreamwithseek.ZipEntryInputStreamWithSeek;



public class BulletinStore
{
	public BulletinStore()
	{
		cacheManager = new BulletinStoreCacheManager();

		leafNodeCache = new LeafNodeCache(this);
		addCache(leafNodeCache);
	}

	public void doAfterSigninInitialization(File dataRootDirectory, Database db) throws FileVerificationException, MissingAccountMapException, MissingAccountMapSignatureException
	{
		dir = dataRootDirectory;
		database = db;
		database.initialize();
	}

	public void setSignatureGenerator(MartusCrypto securityToUse)
	{
		security = securityToUse;
	}
	
	public MartusCrypto getSignatureGenerator()
	{
		return security;
	}
	
	public MartusCrypto getSignatureVerifier()
	{
		return security;
	}
	
	public ReadableDatabase getDatabase()
	{
		return database;
	}
	
	protected Database getWriteableDatabase()
	{
		return database;
	}
	
	public void setDatabase(Database toUse)
	{
		database = toUse;
	}
	
	public File getStoreRootDir()
	{
		return dir;
	}

	public String getAccountId()
	{
		return security.getPublicKeyString();
	}

	public int getBulletinCount()
	{
		return scanForLeafKeys().size();
	}

	public Vector getAllBulletinLeafUids()
	{
		Vector uids = new Vector();
		Vector keys = scanForLeafKeys();
		for(int i=0; i < keys.size(); ++i)
			uids.add( ((DatabaseKey)keys.get(i)).getUniversalId());
		return uids;
	}
	
	public boolean isLeaf(UniversalId uId)
	{
		Vector bulletinLeafUidsInSystem  = getAllBulletinLeafUids();
		return bulletinLeafUidsInSystem.contains(uId);
	}

	public boolean doesBulletinRevisionExist(DatabaseKey key)
	{
		return getDatabase().doesRecordExist(key);
	}
	
	public boolean hasNewerRevision(UniversalId uid)
	{
		return getNonLeafUids().contains(uid);
	}

	public void deleteAllData() throws Exception
	{
		deleteAllBulletins();
	}

	public void deleteAllBulletins() throws Exception
	{
		database.deleteAllData();
		cacheManager.storeWasCleared();
	}

	public void importZipFileToStoreWithSameUids(File inputFile) throws IOException, MartusCrypto.CryptoException, Packet.InvalidPacketException, Packet.SignatureVerificationException
	{
		ZipFile zip = new ZipFile(inputFile);
		try
		{
			importBulletinZipFile(zip);
		}
		catch (Database.RecordHiddenException shouldBeImpossible)
		{
			shouldBeImpossible.printStackTrace();
			throw new IOException(shouldBeImpossible.toString());
		}
		catch(WrongAccountException shouldBeImpossible)
		{
			throw new Packet.InvalidPacketException("Wrong account???");
		}
		finally
		{
			zip.close();
		}
	}

	public void addCache(BulletinStoreCache cacheToAdd)
	{
		cacheManager.addCache(cacheToAdd);
	}
	
	public void revisionWasSaved(UniversalId uid)
	{
		cacheManager.revisionWasSaved(uid);
	}
	
	public void revisionWasSaved(Bulletin b)
	{
		cacheManager.revisionWasSaved(b);
	}
	
	public void revisionWasRemoved(UniversalId uid)
	{
		cacheManager.revisionWasRemoved(uid);
	}
	
	public boolean hadErrorsWhileCacheing()
	{
		return leafNodeCache.hadErrors();
	}
	
	public Vector scanForLeafKeys()
	{
		return leafNodeCache.getLeafKeys();
	}
	
	public Vector getNonLeafUids()
	{
		return leafNodeCache.getNonLeafUids();
	}
	
	public Vector getFieldOffices(String hqAccountId)
	{
		return leafNodeCache.getFieldOffices(hqAccountId);
	}

	public void visitAllBulletins(Database.PacketVisitor visitor)
	{
		Vector leafKeys = scanForLeafKeys();
		for(int i=0; i < leafKeys.size(); ++i)
			visitor.visit((DatabaseKey)leafKeys.get(i));
	}

	public void visitAllBulletinRevisions(Database.PacketVisitor visitorToUse)
	{
		class BulletinKeyFilter implements Database.PacketVisitor
		{
			BulletinKeyFilter(ReadableDatabase db, Database.PacketVisitor visitorToUse2)
			{
				visitor = visitorToUse2;
				db.visitAllRecords(this);
			}
	
			public void visit(DatabaseKey key)
			{
				if(BulletinHeaderPacket.isValidLocalId(key.getLocalId()))
				{
					++count;
					visitor.visit(key);
				}
			}
			ReadableDatabase.PacketVisitor visitor;
			int count;
		}
	
		new BulletinKeyFilter(getDatabase(), visitorToUse);
	}

	public synchronized void removeBulletinFromStore(Bulletin b) throws IOException
	{
		BulletinHistory history = b.getHistory();
		try
		{
			for(int i = 0; i < history.size(); ++i)
			{
				String localIdOfAncestor = history.get(i);
				UniversalId uidOfAncestor = UniversalId.createFromAccountAndLocalId(b.getAccount(), localIdOfAncestor);
				DatabaseKey key = DatabaseKey.createSealedKey(uidOfAncestor);
				if(doesBulletinRevisionExist(key))
					deleteBulletinRevision(key);
			}

			BulletinHeaderPacket bhpMain = b.getBulletinHeaderPacket();
			deleteBulletinRevisionFromDatabase(bhpMain);
		}
		catch(Exception e)
		{
			//e.printStackTrace();
			throw new IOException("Unable to delete bulletin");
		}
	}

	public void deleteBulletinRevision(DatabaseKey keyToDelete) throws IOException, CryptoException, InvalidPacketException, WrongPacketTypeException, SignatureVerificationException, DecryptionException, UnsupportedEncodingException, NoKeyPairException
	{
		BulletinHeaderPacket bhp = loadBulletinHeaderPacket(getDatabase(), keyToDelete, getSignatureVerifier());
		deleteBulletinRevisionFromDatabase(bhp);
	}

	public void deleteBulletinRevisionFromDatabase(BulletinHeaderPacket bhp)
		throws
			IOException,
			MartusCrypto.CryptoException,
			UnsupportedEncodingException,
			Packet.InvalidPacketException,
			Packet.WrongPacketTypeException,
			Packet.SignatureVerificationException,
			MartusCrypto.DecryptionException,
			MartusCrypto.NoKeyPairException
	{
		DatabaseKey[] keys = BulletinZipUtilities.getAllPacketKeys(bhp);
		for (int i = 0; i < keys.length; i++)
		{
			deleteSpecificPacket(keys[i]);
		}
		revisionWasRemoved(bhp.getUniversalId());
	}

	public static BulletinHeaderPacket loadBulletinHeaderPacket(ReadableDatabase db, DatabaseKey key, MartusCrypto security)
	throws
		IOException,
		CryptoException,
		InvalidPacketException,
		WrongPacketTypeException,
		SignatureVerificationException,
		DecryptionException
	{
		InputStreamWithSeek in = db.openInputStream(key, security);
		try
		{
			BulletinHeaderPacket bhp = new BulletinHeaderPacket();
			bhp.loadFromXml(in, security);
			return bhp;
		}
		finally
		{
			in.close();
		}
	}

	public void hidePackets(Vector packetsIdsToHide, LoggerInterface logger) throws InvalidBase64Exception
	{
		Database db = getWriteableDatabase();
		for(int i = 0; i < packetsIdsToHide.size(); ++i)
		{
			UniversalId uId = (UniversalId)(packetsIdsToHide.get(i));
			db.hide(uId);
			revisionWasRemoved(uId);
			String publicCode = MartusCrypto.getFormattedPublicCode(uId.getAccountId());
			logger.logNotice("Deleting " + publicCode + ": " + uId.getLocalId());
		
		}
	}
	
	public void importBulletinZipFile(ZipFile zip) 
		throws InvalidPacketException, 
		SignatureVerificationException, 
		DecryptionException, 
		IOException, 
		RecordHiddenException, 
		WrongAccountException
	{
		importBulletinZipFile(zip, null);
	}

	public void importBulletinZipFile(ZipFile zip, String accountIdIfKnown) 
		throws InvalidPacketException, 
		SignatureVerificationException, 
		DecryptionException, 
		IOException, 
		RecordHiddenException, 
		WrongAccountException
	{
		UniversalId uid = importBulletinPacketsFromZipFileToDatabase(getWriteableDatabase(), accountIdIfKnown, zip, getSignatureVerifier());
		revisionWasSaved(uid);
	}

	private static UniversalId importBulletinPacketsFromZipFileToDatabase(Database db, String authorAccountId, ZipFile zip, MartusCrypto security)
		throws IOException,
		Database.RecordHiddenException,
		Packet.InvalidPacketException,
		Packet.SignatureVerificationException,
		Packet.WrongAccountException,
		MartusCrypto.DecryptionException
	{
		BulletinHeaderPacket header = BulletinHeaderPacket.loadFromZipFile(zip, security);
		if(authorAccountId == null)
			authorAccountId = header.getAccountId();
	
		BulletinZipUtilities.validateIntegrityOfZipFilePackets(authorAccountId, zip, security);
		MartusUtilities.deleteDraftBulletinPackets(db, header.getUniversalId(), security);
	
		HashMap zipEntries = new HashMap();
		StreamCopier copier = new StreamCopier();
		StreamEncryptor encryptor = new StreamEncryptor(security);
	
		DatabaseKey[] keys = BulletinZipUtilities.getAllPacketKeys(header);
		for (int i = 0; i < keys.length; i++)
		{
			String localId = keys[i].getLocalId();
			ZipEntry entry = zip.getEntry(localId);
	
			InputStreamWithSeek in = new ZipEntryInputStreamWithSeek(zip, entry);
	
			final String tempFileName = "$$$importZip";
			File file = File.createTempFile(tempFileName, null);
			file.deleteOnExit();
			FileOutputStream rawOut = new FileOutputStream(file);
	
			StreamFilter filter = copier;
			if(db.mustEncryptLocalData() && MartusUtilities.doesPacketNeedLocalEncryption(localId, header, in))
				filter = encryptor;
	
			MartusUtilities.copyStreamWithFilter(in, rawOut, filter);
	
			rawOut.close();
			in.close();
	
			UniversalId uid = UniversalId.createFromAccountAndLocalId(authorAccountId, keys[i].getLocalId());
			DatabaseKey key = header.createKeyWithHeaderStatus(uid);
	
			zipEntries.put(key,file);
		}
		db.importFiles(zipEntries);
		return header.getUniversalId();
	}

	protected void deleteSpecificPacket(DatabaseKey burKey)
	{
		getWriteableDatabase().discardRecord(burKey);
	}
	
	public void saveBulletinForTesting(Bulletin b) throws IOException, CryptoException
	{
		saveBulletin(b, false);
	}
	
	// TODO: Not sure this method is really needed. I think the tests were 
	// encrypting (mostly) for legacy reasons. We should stamp out all calls
	// to this, at which point we should be able to rename saveBulletinForTesting 
	// to simply saveBulletin, have it trust getDatabase().mustEncryptPublicData(),
	// and then ClientBulletinStore.saveBulletin can just invoke super after clearin its cache
	// kbs. 2004-10-06
	public void saveEncryptedBulletinForTesting(Bulletin b) throws IOException, CryptoException
	{
		saveBulletin(b, true);
	}
	
	protected void saveBulletin(Bulletin b, boolean mustEncryptPublicData) throws IOException, CryptoException
	{
		saveToClientDatabase(b, getWriteableDatabase(), mustEncryptPublicData, b.getSignatureGenerator());
		revisionWasSaved(b);
	}
	
	private static void saveToClientDatabase(Bulletin b, Database db, boolean mustEncryptPublicData, MartusCrypto signer) throws
			IOException,
			MartusCrypto.CryptoException
	{
		UniversalId uid = b.getUniversalId();
		BulletinHeaderPacket oldBhp = new BulletinHeaderPacket(uid);
		DatabaseKey key = DatabaseKey.createLegacyKey(uid);
		boolean bulletinAlreadyExisted = false;
		try
		{
			if(db.doesRecordExist(key))
			{
				oldBhp = loadBulletinHeaderPacket(db, key, signer);
				bulletinAlreadyExisted = true;
			}
		}
		catch(Exception ignoreItBecauseWeCantDoAnythingAnyway)
		{
			//e.printStackTrace();
			//System.out.println("Bulletin.saveToDatabase: " + e);
		}
	
		BulletinHeaderPacket bhp = b.getBulletinHeaderPacket();
	
		FieldDataPacket publicDataPacket = b.getFieldDataPacket();
		boolean shouldEncryptPublicData = (b.isDraft() || b.isAllPrivate());
		publicDataPacket.setEncrypted(shouldEncryptPublicData);
		
		Packet packet1 = publicDataPacket;
		boolean encryptPublicData = mustEncryptPublicData;
		byte[] dataSig = packet1.writeXmlToClientDatabase(db, encryptPublicData, signer);
		bhp.setFieldDataSignature(dataSig);
		
		Packet packet2 = b.getPrivateFieldDataPacket();
		boolean encryptPublicData1 = mustEncryptPublicData;
		byte[] privateDataSig = packet2.writeXmlToClientDatabase(db, encryptPublicData1, signer);
		bhp.setPrivateFieldDataSignature(privateDataSig);
	
		writePendingAttachments(b.getPendingPublicAttachments(), db, mustEncryptPublicData, signer);
		writePendingAttachments(b.getPendingPrivateAttachments(), db, mustEncryptPublicData, signer);
	
		bhp.updateLastSavedTime();
		Packet packet = bhp;
		packet.writeXmlToClientDatabase(db, mustEncryptPublicData, signer);
	
		if(bulletinAlreadyExisted)
		{
			String accountId = b.getAccount();
			String[] oldPublicAttachmentIds = oldBhp.getPublicAttachmentIds();
			String[] newPublicAttachmentIds = bhp.getPublicAttachmentIds();
			BulletinStore.deleteRemovedPackets(db, accountId, oldPublicAttachmentIds, newPublicAttachmentIds);
	
			String[] oldPrivateAttachmentIds = oldBhp.getPrivateAttachmentIds();
			String[] newPrivateAttachmentIds = bhp.getPrivateAttachmentIds();
			BulletinStore.deleteRemovedPackets(db, accountId, oldPrivateAttachmentIds, newPrivateAttachmentIds);
		}
	}

	private static void writePendingAttachments(PendingAttachmentList pendingPublicAttachments, Database db, boolean mustEncryptPublicData, MartusCrypto signer) throws IOException, CryptoException
	{
		for(int i = 0; i < pendingPublicAttachments.size(); ++i)
		{
			// TODO: Should the bhp also remember attachment sigs?
			Packet packet = pendingPublicAttachments.get(i);
			packet.writeXmlToClientDatabase(db, mustEncryptPublicData, signer);
		}
	}

	private static void deleteRemovedPackets(Database db, String accountId, String[] oldIds, String[] newIds)
	{
		for(int oldIndex = 0; oldIndex < oldIds.length; ++oldIndex)
		{
			String oldLocalId = oldIds[oldIndex];
			if(!MartusUtilities.isStringInArray(newIds, oldLocalId))
			{
				UniversalId auid = UniversalId.createFromAccountAndLocalId(accountId, oldLocalId);
				db.discardRecord(DatabaseKey.createLegacyKey(auid));
			}
		}
	}
	
	private MartusCrypto security;
	private File dir;
	private Database database;
	private LeafNodeCache leafNodeCache;
	private BulletinStoreCacheManager cacheManager;
}
