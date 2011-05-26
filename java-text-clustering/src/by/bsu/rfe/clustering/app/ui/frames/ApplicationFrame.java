package by.bsu.rfe.clustering.app.ui.frames;

import by.bsu.rfe.clustering.algorithm.FlatClustering;
import by.bsu.rfe.clustering.algorithm.cluster.Cluster;
import by.bsu.rfe.clustering.nlp.WordList;
import by.bsu.rfe.clustering.text.algorithm.TextKMeansClustering;
import by.bsu.rfe.clustering.text.data.DocumentDataElement;
import by.bsu.rfe.clustering.text.data.DocumentDataSet;
import by.bsu.rfe.clustering.text.ir.Document;
import by.bsu.rfe.clustering.text.ir.DocumentCollection;
import by.bsu.rfe.clustering.text.ir.DocumentCollectionReader;
import by.bsu.rfe.clustering.text.ir.FileSystemDocumentCollectionReader;
import by.bsu.rfe.clustering.text.ir.InformationRetrievalException;
import by.bsu.rfe.clustering.text.vsm.DocumentVSMGenerator;
import by.bsu.rfe.clustering.text.vsm.NormalizedTFIDF;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeSelectionModel;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class ApplicationFrame extends JFrame {

  private static final long serialVersionUID = -3331823388635050038L;

  private static final int DEFAULT_WIDTH = 640;
  private static final int DEFAULT_HEIGHT = 480;

  private final JPanel _mainPanel = new JPanel();

  private final JMenuBar _menuBar = new JMenuBar();

  private final JMenu _menuFile = new JMenu("File");
  private final JMenuItem _menuItemLoad = new JMenuItem("Cluster Documents");

  private final JTree _clusterTree = new JTree();
  private final JScrollPane _treePanel = new JScrollPane(_clusterTree);

  private final JPanel _leftPanel = new JPanel();
  private final JPanel _rightPanel = new JPanel();

  private final JSplitPane _splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, _leftPanel, _rightPanel);

  private final JTextArea _documentViewArea = new JTextArea();

  private DocumentCollection _documentCollection;

  public ApplicationFrame() {
    setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    Frames.center(this);
    init();
    initActionHandlers();
  }

  private void init() {
    _clusterTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

    _mainPanel.setLayout(new BorderLayout());

    _splitPane.setBorder(BorderFactory.createLineBorder(Color.black));
    _splitPane.setDividerSize(2);

    // _leftPanel.setSize(100, _leftPanel.getHeight());

    _leftPanel.setLayout(new BorderLayout());
    _leftPanel.add(_treePanel, BorderLayout.WEST);

    _treePanel.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    _treePanel.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

    getContentPane().add(_mainPanel);
    _mainPanel.add(_splitPane, BorderLayout.CENTER);

    setJMenuBar(_menuBar);

    _menuBar.add(_menuFile);

    _menuFile.add(_menuItemLoad);

    _clusterTree.setModel(new DefaultTreeModel(new DefaultMutableTreeNode("Root")));

    _documentViewArea.setEditable(false);

    _rightPanel.setLayout(new BorderLayout(10, 10));
    _rightPanel.add(_documentViewArea, BorderLayout.CENTER);

    _documentViewArea.setVisible(false);
    _documentViewArea.setLineWrap(true);
    _documentViewArea.setWrapStyleWord(true);
    _documentViewArea.setAutoscrolls(true);
  }

  private void initActionHandlers() {
    _menuItemLoad.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        _leftPanel.removeAll();

        _leftPanel.add(new JLabel("Loading..."), BorderLayout.CENTER);
        _mainPanel.revalidate();
        loadAndClusterDocuments();
      }
    });

    _leftPanel.addComponentListener(new ComponentListener() {
      @Override
      public void componentResized(ComponentEvent e) {
        _clusterTree.setSize(_leftPanel.getWidth(), _clusterTree.getHeight());
      }

      @Override
      public void componentMoved(ComponentEvent e) {
      }

      @Override
      public void componentShown(ComponentEvent e) {
      }

      @Override
      public void componentHidden(ComponentEvent e) {
      }
    });

    _clusterTree.addTreeSelectionListener(new TreeSelectionListener() {

      @Override
      public void valueChanged(TreeSelectionEvent e) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) _clusterTree.getLastSelectedPathComponent();

        if (node != null) {
          Object nodeDataObject = node.getUserObject();

          if (nodeDataObject instanceof Document) {
            Document doc = (Document) nodeDataObject;
            _documentViewArea.setVisible(true);
            _documentViewArea.setText(doc.getOriginalText());
          }
          else {
            _documentViewArea.setText("");
            _documentViewArea.setVisible(false);
          }
        }
      }
    });
  }

  private void loadAndClusterDocuments() {

    // Choose a Folder with text documents
    JFileChooser fileChooser = new JFileChooser(new File("."));

    fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    fileChooser.setAcceptAllFileFilterUsed(false);
    fileChooser.setDialogTitle("Choose a folder");

    fileChooser.setFileFilter(new FileFilter() {
      @Override
      public boolean accept(File f) {
        return f.isDirectory();
      }

      @Override
      public String getDescription() {
        return "Folder with text documents";
      }
    });

    fileChooser.showOpenDialog(this);

    File folder = fileChooser.getSelectedFile();

    if (folder != null) {
      File stopWords = new File("dictionary\\stopwords.txt");
      WordList stopWordList = null;
      try {
        stopWordList = WordList.load(stopWords);
      }
      catch (IOException e) {
        // TODO: handle exception properly
      }

      DocumentCollectionReader reader = new FileSystemDocumentCollectionReader(folder, stopWordList);
      DocumentCollection docCollection = null;

      try {
        docCollection = reader.readDocuments();

        DocumentVSMGenerator vsmGen = new NormalizedTFIDF();
        DocumentDataSet dataSet = vsmGen.createVSM(docCollection);

        final int numberOfClusters = 40;

        FlatClustering<DocumentDataElement, Cluster<DocumentDataElement>, DocumentDataSet> clustering = new TextKMeansClustering(
            numberOfClusters);

        List<Cluster<DocumentDataElement>> clusters = clustering.cluster(dataSet);
        displayClusters(clusters);

      }
      catch (InformationRetrievalException e) {
        // TODO handle exception
      }

    }

  }

  private void displayClusters(List<Cluster<DocumentDataElement>> clusters) {
    _leftPanel.removeAll();
    _clusterTree.setModel(createTreeModel(clusters));
    _leftPanel.add(_treePanel, BorderLayout.WEST);
    _leftPanel.revalidate();
  }

  private TreeModel createTreeModel(List<Cluster<DocumentDataElement>> clusters) {
    DefaultMutableTreeNode root = new DefaultMutableTreeNode("Dataset");

    for (Cluster<DocumentDataElement> c : clusters) {
      DefaultMutableTreeNode clusterNode = new DefaultMutableTreeNode(c.getLabel());
      root.add(clusterNode);

      for (DocumentDataElement elem : c.getDataElements()) {
        Document doc = elem.getDocument();

        DefaultMutableTreeNode documentNode = new DefaultMutableTreeNode(doc);

        clusterNode.add(documentNode);
      }
    }

    return new DefaultTreeModel(root);
  }

}